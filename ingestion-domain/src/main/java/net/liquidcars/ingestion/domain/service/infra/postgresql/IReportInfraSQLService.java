package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportFilterDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportPageDto;

import java.util.List;
import java.util.UUID;

public interface IReportInfraSQLService {
    void upsertIngestionReport(IngestionReportDto ingestionReportDto);

    IngestionReportDto findIngestionReportById(UUID id);

    IngestionReportPageDto findIngestionReports(IngestionReportFilterDto filter);

    IngestionReportDto findIngestionReportByBatchJobId(UUID batchJobId);

    boolean existsByPhysicalInventoryIdAndStatusNotIn(UUID inventoryId, List<IngestionBatchStatus> statuses);

    List<IngestionReportDto> getPendingReports();

}
