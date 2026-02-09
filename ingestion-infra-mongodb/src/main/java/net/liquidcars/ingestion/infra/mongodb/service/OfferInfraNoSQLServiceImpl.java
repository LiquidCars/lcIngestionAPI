package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.OfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraNoSQLServiceImpl implements IOfferInfraNoSQLService {

    private final OfferNoSqlRepository repository;
    private final OfferInfraNoSQLMapper offerInfraNoSQLMapper;

    @Override
    public void processOffer(OfferDto offer) {
        log.info("Processing NoSQL persistence for externalId: {}", offer.getExternalId());

        try {
            OfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);
            repository.findByExternalId(offer.getExternalId())
                    .ifPresentOrElse(
                            existingOffer -> updateIfNewer(existingOffer, entity),
                            () -> repository.save(entity)
                    );

        } catch (Exception e) {
            log.error("Failed to persist offer in NoSQL database. ExternalID: {}", offer.getExternalId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL persistence error for externalId: " + offer.getExternalId())
                    .cause(e)
                    .build();
        }
    }

    private void updateIfNewer(OfferNoSQLEntity existing, OfferNoSQLEntity incoming) {
        if (incoming.getCreatedAt().isAfter(existing.getCreatedAt())) {
            log.debug("Updating existing offer. ExternalID: {}", incoming.getExternalId());
            incoming.setId(existing.getId());
            repository.save(incoming);
        } else {
            log.debug("Incoming offer is older than existing one. Skipping update. ExternalID: {}", incoming.getExternalId());
        }
    }

    @Override
    public void processIngestionReport(IngestionReportDto ingestionReportDto) {
        log.info("Processing NoSQL Report for Job: {}", ingestionReportDto.getJobId());
        try {
            if ("FAILED".equalsIgnoreCase(ingestionReportDto.getStatus())) {
                handleFailedJobCleanup(ingestionReportDto);
            } else if ("COMPLETED".equalsIgnoreCase(ingestionReportDto.getStatus())) {
                handleSuccessfulJobUpdate(ingestionReportDto);
            } else {
                log.warn("Unexpected job status: {} for Job: {}",
                        ingestionReportDto.getStatus(), ingestionReportDto.getJobId());
            }
        } catch (LCIngestionException e) {
            throw e; // Triggering Kafka retry
        } catch (Exception e) {
            log.error("Failed to update NoSQL for Job: {}", ingestionReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL report update error")
                    .cause(e)
                    .build();
        }
    }

    private void handleFailedJobCleanup(IngestionReportDto report) {
        log.warn("Job {} FAILED. Initiating NoSQL cleanup of partial data.", report.getJobId());

        try {
            long currentOffersInDb = repository.countByJobIdentifier(report.getJobId());

            log.info("NoSQL Job {}: writeCount={}, current offers in DB={}",
                    report.getJobId(), report.getWriteCount(), currentOffersInDb);


            // Race condition detection:
            // Case 1: writeCount > 0 but no offers in DB yet (offers still in transit)
            // Case 2: writeCount = 0 but readCount > 0 (job failed mid-chunk, offers may have been sent to Kafka)
            boolean possibleRaceCondition = (report.getWriteCount() > 0 && currentOffersInDb == 0) ||
                    (report.getWriteCount() == 0 && report.getReadCount() > 0 && currentOffersInDb == 0);

            if (possibleRaceCondition) {
                log.warn("NoSQL Race condition suspected for Job {}. No offers found yet but writeCount={}. Retrying...",
                        report.getJobId(), report.getWriteCount());
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                        .message("Cleanup race condition: offers not yet visible in NoSQL DB")
                        .build();
            }

            if (currentOffersInDb > 0) {
                long deletedCount = repository.deleteByJobIdentifier(report.getJobId());
                log.info("Successfully deleted {} NoSQL offers for FAILED Job: {}",
                        deletedCount, report.getJobId());

                if (deletedCount != currentOffersInDb) {
                    log.warn("NoSQL Inconsistency detected: counted {} offers but deleted {}. " +
                                    "This may indicate concurrent processing.",
                            currentOffersInDb, deletedCount);
                }
            } else {
                log.info("No NoSQL offers found to clean for Job {}. Job may have failed before processing any records.",
                        report.getJobId());
            }

        } catch (LCIngestionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during NoSQL cleanup of FAILED Job: {}", report.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to cleanup NoSQL after job failure")
                    .cause(e)
                    .build();
        }
    }

    private void handleSuccessfulJobUpdate(IngestionReportDto report) {
        try {
            repository.updateBatchStatusByJobIdentifier(report.getJobId(), "COMPLETED");
            log.info("Successfully updated NoSQL records to COMPLETED for Job {}", report.getJobId());
        } catch (Exception e) {
            log.error("Error updating NoSQL COMPLETED status for Job: {}", report.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to update NoSQL job completion status")
                    .cause(e)
                    .build();
        }
    }

    @Override
    public void purgeObsoleteOffers(int daysOld) {
        Instant threshold = Instant.now().minus(daysOld, ChronoUnit.DAYS);

        log.info("Starting purge of obsolete offers. Criteria: batchStatus != 'COMPLETED' AND updatedAt < {}", threshold);

        try {
            /*
             * We execute a bulk delete operation. Using a single query with $ne and $lt
             * is highly efficient as MongoDB performs the filter and deletion in one pass.
             */
            long offersDeleted = repository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(threshold);
            log.info("Obsolete offers purge completed successfully. Deleted {} offers", offersDeleted);
        } catch (Exception e) {
            log.error("Failed to purge obsolete offers from NoSQL", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error during NoSQL offers data purge")
                    .cause(e)
                    .build();
        }
    }


}
