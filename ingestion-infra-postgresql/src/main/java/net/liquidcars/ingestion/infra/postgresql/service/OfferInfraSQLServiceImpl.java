package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {

    private final OfferSQLRepository offerSQLRepository;
    private final IngestionReportRepository reportRepository;
    private final OfferInfraSQLMapper mapper;


    @Override
    @Transactional // Vital to ensure the find + save is consistent
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
        log.info("Processing SQL Report and updating offers for Job: {}", ingestionReportDto.getJobId());

        try {
            // 1. Persist the report itself in ingestion_reports table
            reportRepository.save(mapper.toIngestionReportEntity(ingestionReportDto));

            // 2. Handle associated offers based on status
            if ("FAILED".equalsIgnoreCase(ingestionReportDto.getStatus())) {
                log.warn("Job {} FAILED. Purging partial SQL data for this execution.", ingestionReportDto.getJobId());
                offerSQLRepository.deleteByJobIdentifier(ingestionReportDto.getJobId());
            } else {
                log.debug("Updating batchStatus to {} in SQL for Job: {}", ingestionReportDto.getStatus(), ingestionReportDto.getJobId());
                offerSQLRepository.updateBatchStatusByJobIdentifier(ingestionReportDto.getJobId(), ingestionReportDto.getStatus());
            }

        } catch (Exception e) {
            log.error("Failed to process SQL ingestion report for Job: {}", ingestionReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL report processing error for Job: " + ingestionReportDto.getJobId())
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
            offerSQLRepository.deleteObsoleteOffers(threshold);
            log.info("SQL purge completed successfully.");
        } catch (Exception e) {
            log.error("Failed to purge obsolete SQL data", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error during SQL data purge")
                    .cause(e)
                    .build();
        }
    }

}
