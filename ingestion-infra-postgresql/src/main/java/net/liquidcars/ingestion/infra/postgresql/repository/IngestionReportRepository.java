package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IngestionReportRepository extends JpaRepository<IngestionReportEntity, UUID> {

    Optional<IngestionReportEntity> findByBatchJobId(UUID batchJobId);

    boolean existsByRequesterParticipantIdAndStatusNotIn(UUID requesterParticipantId, List<IngestionBatchStatus> statuses);

    List<IngestionReportEntity> findByProcessedFalse();
}