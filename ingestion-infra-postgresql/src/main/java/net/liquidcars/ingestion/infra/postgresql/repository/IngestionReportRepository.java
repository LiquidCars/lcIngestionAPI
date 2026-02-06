package net.liquidcars.ingestion.infra.postgresql.repository;

import net.liquidcars.ingestion.infra.postgresql.entity.IngestionReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionReportRepository extends JpaRepository<IngestionReportEntity, String> {

}
