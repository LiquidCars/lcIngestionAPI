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

            // 2. Handle failure cleanup with eventual consistency check
            if ("FAILED".equalsIgnoreCase(ingestionReportDto.getStatus())) {
                handleFailedJobCleanup(ingestionReportDto);
            } else if ("COMPLETED".equalsIgnoreCase(ingestionReportDto.getStatus())) {
                handleSuccessfulJobUpdate(ingestionReportDto);
            } else {
                log.warn("Unexpected job status: {} for Job: {}",
                        ingestionReportDto.getStatus(), ingestionReportDto.getJobId());
            }

        } catch (LCIngestionException e) {
            // Rethrow to let Kafka ErrorHandler/Retry handle it
            throw e;
        } catch (Exception e) {
            log.error("Critical error processing SQL report for Job: {}", ingestionReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL report processing error")
                    .cause(e)
                    .build();
        }
    }

    private void handleFailedJobCleanup(IngestionReportDto report) {
        log.warn("Job {} FAILED. Initiating cleanup of partial data.", report.getJobId());

        try {
            int currentOffersInDb = offerSQLRepository.countByJobIdentifier(report.getJobId());

            log.info("Job {}: writeCount={}, current offers in DB={}",
                    report.getJobId(), report.getWriteCount(), currentOffersInDb);

            // Race condition detection:
            // Case 1: writeCount > 0 but no offers in DB yet (offers still in transit)
            // Case 2: writeCount = 0 but readCount > 0 (job failed mid-chunk, offers may have been sent to Kafka)
            boolean possibleRaceCondition = (report.getWriteCount() > 0 && currentOffersInDb == 0) ||
                    (report.getWriteCount() == 0 && report.getReadCount() > 0 && currentOffersInDb == 0);

            if (possibleRaceCondition) {
                log.warn("Race condition suspected for Job {}. No offers found yet but writeCount={}. Retrying...",
                        report.getJobId(), report.getWriteCount());
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                        .message("Cleanup race condition: offers not yet visible in DB")
                        .build();
            }

            if (currentOffersInDb > 0) {
                int deletedCount = offerSQLRepository.deleteByJobIdentifier(report.getJobId());
                log.info("Successfully purged {} records for FAILED Job {}",
                        deletedCount, report.getJobId());

                if (deletedCount != currentOffersInDb) {
                    log.warn("Inconsistency detected: counted {} offers but deleted {}. " +
                                    "This may indicate concurrent processing.",
                            currentOffersInDb, deletedCount);
                }
            } else {
                log.info("No offers found to clean for Job {}. Job may have failed before processing any records.",
                        report.getJobId());
            }

        } catch (LCIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during cleanup of FAILED Job: {}", report.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to cleanup after job failure")
                    .cause(e)
                    .build();
        }
    }

    private void handleSuccessfulJobUpdate(IngestionReportDto report) {
        try {
            int offersUpdated = offerSQLRepository.updateBatchStatusByJobIdentifier(
                    report.getJobId(),
                    "COMPLETED"
            );

            log.info("Successfully updated {} records to COMPLETED for Job {}",
                    offersUpdated, report.getJobId());

            if (offersUpdated != report.getWriteCount()) {
                log.warn("Consistency check: writeCount={} but updated={} records for Job {}. " +
                                "This may indicate data loss or concurrent updates.",
                        report.getWriteCount(), offersUpdated, report.getJobId());
            }

        } catch (Exception e) {
            log.error("Error updating COMPLETED status for Job: {}", report.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to update job completion status")
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

}
