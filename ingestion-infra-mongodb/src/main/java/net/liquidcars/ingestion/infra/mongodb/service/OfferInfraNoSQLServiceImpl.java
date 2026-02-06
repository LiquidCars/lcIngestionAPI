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
        log.info("Updating batchStatus to '{}' for all offers linked to Job: {}",
                ingestionReportDto.getStatus(), ingestionReportDto.getJobId());
        try {
            if ("FAILED".equalsIgnoreCase(ingestionReportDto.getStatus())) {
                /*
                 * If the job failed, we must ensure data consistency by removing all partial
                 * data ingested during this specific execution.
                 * This prevents having "orphan" or corrupted data in the NoSQL store.
                 */
                log.warn("Job {} FAILED. Deleting all associated offers from NoSQL.", ingestionReportDto.getJobId());
                repository.deleteByJobIdentifier(ingestionReportDto.getJobId());
                log.info("Successfully deleted offers for failed Job with id: {}", ingestionReportDto.getJobId());

            } else {
                repository.updateBatchStatusByJobIdentifier(ingestionReportDto.getJobId(), ingestionReportDto.getStatus());
                log.debug("NoSQL batchStatus update completed for Job: {}", ingestionReportDto.getJobId());
            }
        } catch (Exception e) {
            log.error("Failed to update batchStatus in NoSQL for Job: {}", ingestionReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL report update error for Job: " + ingestionReportDto.getJobId())
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
            repository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(threshold);
            log.info("Obsolete offers purge completed successfully.");
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
