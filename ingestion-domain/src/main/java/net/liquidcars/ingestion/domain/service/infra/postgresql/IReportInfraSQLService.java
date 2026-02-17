package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportFilterDto;

import java.util.List;
import java.util.UUID;

public interface IReportInfraSQLService {
    void upsertIngestionReport(IngestionReportDto ingestionReportDto);

    IngestionReportDto findIngestionReportById(UUID id);

    List<IngestionReportDto> findIngestionReports(IngestionReportFilterDto filter);

    IngestionReportDto findIngestionReportByBatchJobId(UUID batchJobId);

    boolean existsByRequesterParticipantIdAndStatusNotIn(UUID requesterParticipantId, List<IngestionBatchStatus> statuses);

    List<IngestionReportDto> getPendingReports();

}
