package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {

    private final OfferSQLRepository offerSQLRepository;
    private final IngestionReportRepository reportRepository;
    private final OfferInfraSQLMapper mapper;

    @Override
    @Transactional
    public void processOffer(OfferDto offer) {
        log.info("Processing SQL persistence for externalId: {}", offer.getExternalId());

        try {
            OfferEntity entity = mapper.toOfferEntity(offer);
            offerSQLRepository.findByExternalId(offer.getExternalId())
                    .ifPresentOrElse(
                            existingOffer -> updateIfNewer(existingOffer, entity),
                            () -> offerSQLRepository.save(entity)
                    );

        } catch (Exception e) {
            log.error("Failed to persist offer in SQL database. ExternalID: {}", offer.getExternalId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence error for externalId: " + offer.getExternalId())
                    .cause(e)
                    .build();
        }
    }

    private void updateIfNewer(OfferEntity existing, OfferEntity incoming) {
        if (incoming.getCreatedAt().isAfter(existing.getCreatedAt())) {
            log.debug("Updating existing offer. ExternalID: {}", incoming.getExternalId());
            incoming.setId(existing.getId());
            offerSQLRepository.save(incoming);
        } else {
            log.debug("Incoming offer is older than existing one. Skipping update. ExternalID: {}", incoming.getExternalId());
        }
    }

    @Override
    @Transactional
    public void processIngestionReport(IngestionReportDto ingestionReportDto) {
        log.info("Processing SQL Report for Job: {}", ingestionReportDto.getJobId());

        try {
            // 1. Persist the report record
            reportRepository.save(mapper.toIngestionReportEntity(ingestionReportDto));
        } catch (Exception e) {
            log.error("Critical error processing SQL report for Job: {}", ingestionReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL report processing error")
                    .cause(e)
                    .build();
        }
    }

    @Override
    @Transactional
    public void purgeObsoleteOffers(int daysOld) {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(daysOld);
        log.info("Starting SQL purge. Criteria: batchStatus != 'COMPLETED' AND updatedAt < {}", threshold);

        try {
            // Purge obsolete offers
            int offersDeleted = offerSQLRepository.deleteObsoleteOffers(threshold);
            log.info("SQL purge completed successfully. Deleted {} offers", offersDeleted);
        } catch (Exception e) {
            log.error("Failed to purge obsolete SQL data", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error during SQL data purge")
                    .cause(e)
                    .build();
        }
    }

    @Override
    public List<IngestionReportDto> getPendingReports(){
        return mapper.toIngestionReportDtoList(reportRepository.findByProcessedFalse());
    }

    @Transactional
    @Override
    public void syncPendingReports(List<IngestionReportDto> pendingReports) {
        for (IngestionReportDto report : pendingReports) {
            try {
                if ("FAILED".equals(report.getStatus())) {
                    offerSQLRepository.deleteByJobIdentifier(report.getJobId());
                } else {
                    offerSQLRepository.updateBatchStatusByJobIdentifier(report.getJobId(), "COMPLETED");
                }
                IngestionReportEntity reportEntity = mapper.toIngestionReportEntity(report);
                reportEntity.setProcessed(true);
                reportRepository.save(reportEntity);
            } catch (Exception e) {
                log.error("Error syncing report {}", report.getJobId(), e);
            }
        }
    }

}
