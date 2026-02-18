package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.SortDirection;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IReportInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.ReportInfraSQLMapper;
import net.liquidcars.ingestion.infra.postgresql.specification.IngestionReportSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
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
            Optional<IngestionReportEntity> optionalIngestionReportEntity = reportRepository.findById(id);
            if(optionalIngestionReportEntity.isPresent()){
                return mapper.toIngestionReportDto(reportRepository.findById(id).orElseThrow());
            } else {
                log.warn("Failed to get ingestion report in SQL database. ID: {}. Not found", id);
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.NOT_FOUND)
                        .message("SQL error for get ingestion report with ID: " + id)
                        .build();
            }
        } catch (Exception e) {
            if(e instanceof LCIngestionException){
                throw e;
            }
            log.error("Failed to get ingestion report in SQL database. ID: {}", id, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL error for get ingestion report with id: " + id)
                    .cause(e)
                    .build();
        }

    }

    @Override
    public IngestionReportPageDto findIngestionReports(IngestionReportFilterDto filter) {
        try {
            Sort sort = Sort.by(
                    filter.getSortDirection() == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                    filter.getSortBy().name()
            );

            Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), sort);

            Specification<IngestionReportEntity> spec = IngestionReportSpecification.filterBy(filter);

            Page<IngestionReportEntity> entityPage = reportRepository.findAll(spec, pageable);

            return mapper.toIngestionReportPageDto(entityPage);

        } catch (Exception e) {
            log.error("Failed to query ingestion reports with filter: {}", filter, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error querying ingestion reports database")
                    .cause(e)
                    .build();
        }
    }

    @Override
    public IngestionReportDto findIngestionReportByBatchJobId(UUID batchJobId) {
        try {
            Optional<IngestionReportEntity> optionalIngestionReportEntity = reportRepository.findByBatchJobId(batchJobId);
            if(optionalIngestionReportEntity.isPresent()){
                return mapper.toIngestionReportDto(reportRepository.findByBatchJobId(batchJobId).orElseThrow());
            } else {
                log.warn("Failed to get ingestion report in SQL database. batchJobId: {}. Not found", batchJobId);
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.NOT_FOUND)
                        .message("SQL error for get ingestion report with batchJobId: " + batchJobId)
                        .build();
            }

        } catch (Exception e) {
            if(e instanceof LCIngestionException){
                throw e;
            }
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
