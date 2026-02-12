package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.OfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraNoSQLServiceImpl implements IOfferInfraNoSQLService {

    private final OfferNoSqlRepository repository;
    private final OfferInfraNoSQLMapper offerInfraNoSQLMapper;

    @Override
    @Transactional
    public void processOffer(OfferDto offer) {
        log.info("Processing NoSQL persistence for id: {}", offer.getId());

        try {
            OfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);
            entity.setCreatedAt(Instant.now());
            repository.findById(offer.getId().toString())
                    .ifPresentOrElse(
                            existingOffer -> updateIfNewer(existingOffer, entity),
                            () -> repository.save(entity)
                    );

        } catch (Exception e) {
            log.error("Failed to persist offer in NoSQL database. ID: {}", offer.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL persistence error for id: " + offer.getId())
                    .cause(e)
                    .build();
        }
    }

    private void updateIfNewer(OfferNoSQLEntity existing, OfferNoSQLEntity incoming) {
        boolean shouldUpdate = existing.getCreatedAt() == null ||
                incoming.getCreatedAt().isAfter(existing.getCreatedAt());

        if (shouldUpdate) {
            log.debug("Updating existing offer. ID: {}", incoming.getId());
            incoming.setId(existing.getId());
            if (existing.getCreatedAt() != null) {
                incoming.setCreatedAt(existing.getCreatedAt());
            }
            repository.save(incoming);
        } else {
            log.debug("Incoming offer is older than existing one. Skipping update. ExternalID: {}", incoming.getId());
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


    @Override
    public long countOffersFromJobId(UUID jobId){
        try {
            return repository.countByJobIdentifier(jobId);
        } catch (Exception e) {
            log.error("Failed to get offers from NoSQL by jobId: {}", jobId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to get offers from NoSQL by jobId: " + jobId)
                    .cause(e)
                    .build();
        }

    }

    @Override
    public long countOffersFromReportId(UUID ingestionReportId){
        try {
            return repository.countByIngestionReportId(ingestionReportId);
        } catch (Exception e) {
            log.error("Failed to get offers from NoSQL by ingestionReportId: {}", ingestionReportId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to get offers from NoSQL by ingestionReportId: " + ingestionReportId)
                    .cause(e)
                    .build();
        }

    }


}
