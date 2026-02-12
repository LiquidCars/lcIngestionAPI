package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IReportInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.ReportInfraSQLMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportInfraSQLServiceImpl implements IReportInfraSQLService {
    private final ReportInfraSQLMapper mapper;
    private final IngestionReportRepository reportRepository;

    @Override
    public void upsertIngestionReport(IngestionReportDto ingestionReportDto) {
        try {
            reportRepository.save(mapper.toIngestionReportEntity(ingestionReportDto));
        } catch (Exception e) {
            log.error("Failed to persist ingestion report in SQL database. ID: {}", ingestionReportDto.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence error for ingestion report with id: " + ingestionReportDto.getId())
                    .cause(e)
                    .build();
        }

    }

    @Override
    public IngestionReportDto findIngestionReportById(UUID id) {
        try {
           return mapper.toIngestionReportDto(reportRepository.findById(id).orElseThrow());
        } catch (Exception e) {
            log.error("Failed to get ingestion report in SQL database. ID: {}", id, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL  error for get ingestion report with id: " + id)
                    .cause(e)
                    .build();
        }

    }

    @Override
    public IngestionReportDto findIngestionReportByBatchJobId(UUID batchJobId) {
        try {
            return mapper.toIngestionReportDto(reportRepository.findByBatchJobId(batchJobId).orElseThrow());
        } catch (Exception e) {
            log.error("Failed to get ingestion report in SQL database. batchJobId: {}", batchJobId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL  error for get ingestion report with batchJobId: " + batchJobId)
                    .cause(e)
                    .build();
        }

    }

    @Override
    public boolean existsByRequesterParticipantIdAndStatusNotIn(UUID requesterParticipantId, List<IngestionBatchStatus> statuses) {
        try {
            return reportRepository.existsByRequesterParticipantIdAndStatusNotIn(requesterParticipantId, statuses);
        } catch (Exception e) {
            String statusList = (statuses != null)
                    ? statuses.stream().map(Enum::name).collect(Collectors.joining(", "))
                    : "null";
            log.error("Database error checking ingestion status for participant: {} and statuses: {}",
                    requesterParticipantId, statusList, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message(String.format("SQL Failed to check if participant [%s] has reports excluding statuses: [%s]",
                            requesterParticipantId, statusList))
                    .cause(e)
                    .build();
        }

    }

    @Override
    public List<IngestionReportDto> getPendingReports() {
        return mapper.toIngestionReportDtoList(reportRepository.findByProcessedFalse());
    }

}
