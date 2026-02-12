package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;

import java.util.List;

public interface IBatchReportInfraSQLService {
    List<IngestionBatchReportDto> getBatchPendingReports();
    void upsertIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto);
}
