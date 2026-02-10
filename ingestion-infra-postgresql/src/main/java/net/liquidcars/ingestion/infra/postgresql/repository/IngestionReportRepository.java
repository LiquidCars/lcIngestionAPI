package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.IngestionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionReportRepository extends JpaRepository<IngestionReportEntity, String> {
    /**
     * Retrieves all ingestion reports that have not been processed by the scheduler.
     * Use this to identify jobs that need data promotion or cleanup.
     * * @return a list of pending ingestion reports.
     */
    List<IngestionReportEntity> findByProcessedFalse();
}
