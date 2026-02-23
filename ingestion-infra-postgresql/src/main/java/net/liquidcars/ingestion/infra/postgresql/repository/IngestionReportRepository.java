package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionReportRepository extends JpaRepository<IngestionReportEntity, UUID>, JpaSpecificationExecutor<IngestionReportEntity> {

    Optional<IngestionReportEntity> findByBatchJobId(UUID batchJobId);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1 
        FROM public.ingestion_reports ir
        INNER JOIN public.inv_nin_namedinventory inn ON ir.inventory_id = inn.nin_co_id
        WHERE ir.inventory_id = :inventoryId 
          AND inn.nin_bo_physical = true
          AND ir.status NOT IN (:statuses)
    )
    """, nativeQuery = true)
    boolean existsByPhysicalInventoryIdAndStatusNotIn(
            @Param("inventoryId") UUID inventoryId,
            @Param("statuses") List<String> statuses
    );

    List<IngestionReportEntity> findByProcessedFalse();
}