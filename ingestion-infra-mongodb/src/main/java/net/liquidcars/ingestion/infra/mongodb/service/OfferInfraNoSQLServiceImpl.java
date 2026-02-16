package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.VehicleOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.DraftOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.repository.VehicleOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.bson.Document;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraNoSQLServiceImpl implements IOfferInfraNoSQLService {

    private final DraftOfferNoSqlRepository draftOfferNoSqlRepository;
    private final OfferInfraNoSQLMapper offerInfraNoSQLMapper;

    private final MongoTemplate mongoTemplate;
    private final VehicleOfferNoSqlRepository vehicleOfferNoSqlRepository;

    @Override
    @Transactional
    public void processOffer(OfferDto offer) {
        log.info("Processing NoSQL persistence for id: {}", offer.getId());

        try {
            DraftOfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);
            entity.setCreatedAt(Instant.now());
            draftOfferNoSqlRepository.findById(offer.getId())
                    .ifPresentOrElse(
                            existingOffer -> updateIfNewer(existingOffer, entity),
                            () -> draftOfferNoSqlRepository.save(entity)
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

    private void updateIfNewer(DraftOfferNoSQLEntity existing, DraftOfferNoSQLEntity incoming) {
        boolean shouldUpdate = existing.getCreatedAt() == null ||
                incoming.getCreatedAt().isAfter(existing.getCreatedAt());

        if (shouldUpdate) {
            log.debug("Updating existing offer. ID: {}", incoming.getId());
            incoming.setId(existing.getId());
            if (existing.getCreatedAt() != null) {
                incoming.setCreatedAt(existing.getCreatedAt());
            }
            draftOfferNoSqlRepository.save(incoming);
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
            long offersDeleted = draftOfferNoSqlRepository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(threshold);
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
            return draftOfferNoSqlRepository.countByJobIdentifier(jobId);
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
            return draftOfferNoSqlRepository.countByIngestionReportId(ingestionReportId);
        } catch (Exception e) {
            log.error("Failed to get offers from NoSQL by ingestionReportId: {}", ingestionReportId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to get offers from NoSQL by ingestionReportId: " + ingestionReportId)
                    .cause(e)
                    .build();
        }

    }

    @Override
    public void promoteDraftOffersToVehicleOffers(UUID ingestionReportId, IngestionDumpType dumpType, UUID inventoryId, List<String> externalIdsToDelete) {
        log.info("Starting promotion for ingestionReportId: {}", ingestionReportId);
        // Promote logic
        List<UUID> promotedIds = promoteDraftOffersAndGetsPromoted(ingestionReportId, inventoryId);

        // REPLACEMENT logic
        replaceOffers(dumpType, inventoryId, promotedIds);

        // Process explicit deletions (offersToDelete from JSON)
        deleteOffersInPromotion(inventoryId, externalIdsToDelete);
    }

    private List<UUID> promoteDraftOffersAndGetsPromoted(UUID ingestionReportId, UUID inventoryId) {
        // 1. Use a Stream to avoid loading the entire list into memory
        Query draftQuery = new Query(Criteria.where("ingestion_report_id").is(ingestionReportId));

        // List to track processed IDs for the REPLACEMENT logic
        List<UUID> promotedIds = new ArrayList<>();

        // 2. Define batch size for Bulk operations (e.g., every 100 records)
        int batchSize = 100;
        int count = 0;
        int totalPromoted = 0;

        // Use mongoTemplate.stream to open a cursor and process records one by one
        try (Stream<DraftOfferNoSQLEntity> draftStream = mongoTemplate.stream(draftQuery, DraftOfferNoSQLEntity.class)) {

            // Initialize the first bulk operation
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, VehicleOfferNoSQLEntity.class);

            var iterator = draftStream.iterator();
            while (iterator.hasNext()) {

                DraftOfferNoSQLEntity draft = iterator.next();

                log.info("Processing draft offer - ID: {}, ownerRef: {}, dealerRef: {}, channelRef: {}",
                        draft.getId(), draft.getOwnerReference(), draft.getDealerReference(), draft.getChannelReference());

                VehicleOfferNoSQLEntity productionEntity = offerInfraNoSQLMapper.toVehicleOfferNoSQLEntity(draft);
                promotedIds.add(productionEntity.getId());

                // 1. Build the query to find existing record
                // Match by inventory_id AND all present references (using AND logic, not OR)
                List<Criteria> andCriteria = new ArrayList<>();
                andCriteria.add(Criteria.where("inventory_id").is(inventoryId));

                boolean hasAnyReference = false;

                // Build AND criteria: match all references that exist, consider null for those that don't
                if (draft.getOwnerReference() != null && !draft.getOwnerReference().isEmpty()) {
                    andCriteria.add(Criteria.where("owner_reference").is(draft.getOwnerReference()));
                    hasAnyReference = true;
                } else {
                    andCriteria.add(new Criteria().orOperator(
                            Criteria.where("owner_reference").is(null),
                            Criteria.where("owner_reference").exists(false)
                    ));
                }

                if (draft.getDealerReference() != null && !draft.getDealerReference().isEmpty()) {
                    andCriteria.add(Criteria.where("dealer_reference").is(draft.getDealerReference()));
                    hasAnyReference = true;
                } else {
                    andCriteria.add(new Criteria().orOperator(
                            Criteria.where("dealer_reference").is(null),
                            Criteria.where("dealer_reference").exists(false)
                    ));
                }

                if (draft.getChannelReference() != null && !draft.getChannelReference().isEmpty()) {
                    andCriteria.add(Criteria.where("channel_reference").is(draft.getChannelReference()));
                    hasAnyReference = true;
                } else {
                    andCriteria.add(new Criteria().orOperator(
                            Criteria.where("channel_reference").is(null),
                            Criteria.where("channel_reference").exists(false)
                    ));
                }

                Query upsertQuery;

                // If we have at least one reference, use the AND criteria
                // Otherwise, use ID to ensure new record insertion
                if (hasAnyReference) {
                    upsertQuery = new Query(new Criteria().andOperator(andCriteria.toArray(new Criteria[0])));
                } else {
                    // No references available at all - use ID to ensure new record insertion
                    upsertQuery = new Query(Criteria.where("_id").is(productionEntity.getId()));
                }

                // 2. Prepare the Update object using $set for all fields
                Document doc = new Document();
                mongoTemplate.getConverter().write(productionEntity, doc);

                // Remove both _id and _class - never update _id in MongoDB
                doc.remove("_id");
                doc.remove("_class"); // Avoid inheritance issues

                Update update = new Update();

                // Add all fields EXCEPT _id
                for (String key : doc.keySet()) {
                    Object value = doc.get(key);
                    if (value != null) {
                        update.set(key, value);
                    }
                }

                // 3. Add to bulk
                bulkOps.upsert(upsertQuery, update);

                count++;

                // Execute batch when reaching batch size
                if (count >= batchSize) {
                    log.info("Executing bulk of {} operations", count);
                    try {
                        var result = bulkOps.execute();
                        totalPromoted += count;
                        log.info("Bulk execution result - Inserted: {}, Modified: {}",
                                result.getInsertedCount(), result.getModifiedCount());
                    } catch (Exception e) {
                        log.error("Error executing bulk operation", e);
                        throw e;
                    }

                    // Reset for next batch
                    bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, VehicleOfferNoSQLEntity.class);
                    count = 0;
                }
            }

            // Execute any remaining operations in the last batch
            if (count > 0) {
                log.debug("Executing final bulk of {} operations", count);
                try {
                    var result = bulkOps.execute();
                    totalPromoted += count;
                    log.debug("Final bulk execution result - Inserted: {}, Modified: {}",
                            result.getInsertedCount(), result.getModifiedCount());
                } catch (Exception e) {
                    log.error("Error executing final bulk operation", e);
                    throw e;
                }
            }

            log.info("Promoted {} draft offers to vehicle offers for ingestionReportId: {}", totalPromoted, ingestionReportId);
        }
        return promotedIds;
    }

    private void replaceOffers(IngestionDumpType dumpType, UUID inventoryId, List<UUID> promotedIds) {
        if (dumpType == IngestionDumpType.REPLACEMENT) {
            log.info("Executing REPLACEMENT cleanup for inventoryId {}", inventoryId);
            // Delete offers in production for this participant that were NOT processed in this Job
            vehicleOfferNoSqlRepository.deleteByInventoryIdAndIdNotIn(inventoryId, promotedIds);
        }
    }

    private void deleteOffersInPromotion(UUID inventoryId, List<String> externalIdsToDelete) {
        if (externalIdsToDelete != null && !externalIdsToDelete.isEmpty()) {
            log.info("Processing {} explicit deletions for inventory {}", externalIdsToDelete.size(), inventoryId);

            // Build query to match records where ANY of the three references match the deletion list
            // This is correct with OR because we want to delete if ANY reference matches
            Criteria orDeleteCriteria = new Criteria().orOperator(
                    Criteria.where("owner_reference").in(externalIdsToDelete),
                    Criteria.where("dealer_reference").in(externalIdsToDelete),
                    Criteria.where("channel_reference").in(externalIdsToDelete)
            );

            Query deleteQuery = new Query(new Criteria().andOperator(
                    Criteria.where("inventory_id").is(inventoryId),
                    orDeleteCriteria
            ));

            long deletedCount = mongoTemplate.remove(deleteQuery, VehicleOfferNoSQLEntity.class).getDeletedCount();
            log.info("Explicitly deleted {} offers from production", deletedCount);
        }
    }

    @Override
    public void deleteDraftOffersByIngestionReportId(UUID ingestionReportId) {
        log.info("Starting delete offers by ingestionReportId: {}", ingestionReportId);
        long deletedCount = draftOfferNoSqlRepository.deleteByIngestionReportId(ingestionReportId);
        log.info("Deleted: {} offers with ingestionReportId: {}", deletedCount, ingestionReportId);
    }


}
