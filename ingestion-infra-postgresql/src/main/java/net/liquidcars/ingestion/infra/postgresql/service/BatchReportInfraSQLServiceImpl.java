package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IBatchReportInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionBatchReportRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.BatchReportInfraSQLMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchReportInfraSQLServiceImpl implements IBatchReportInfraSQLService {
    private final BatchReportInfraSQLMapper mapper;
    private final IngestionBatchReportRepository reportRepository;

    @Override
    public List<IngestionBatchReportDto> getBatchPendingReports(){
        return mapper.toIngestionReportDtoList(reportRepository.findByProcessedFalse());
    }

    @Override
    public void upsertIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto) {
        try {
            reportRepository.save(mapper.toIngestionReportEntity(ingestionBatchReportDto));
        } catch (Exception e) {
            log.error("Failed to persist batch report in SQL database. ID: {}", ingestionBatchReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence error for batch report with id: " + ingestionBatchReportDto.getJobId())
                    .cause(e)
                    .build();
        }

    }

}
