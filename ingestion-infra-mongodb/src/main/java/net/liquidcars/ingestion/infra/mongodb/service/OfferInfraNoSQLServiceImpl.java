package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    private final IOfferInfraSQLService offerInfraSQLService;
    private final TransactionTemplate transactionTemplate;

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

            // 1. Promote to NoSQL (vehicle offers collection)
            List<UUID> promotedNoSQLIds = promoteDraftOffersAndGetsPromoted(ingestionReportId, inventoryId);

            // 2. Promote to SQL (PostgreSQL) and get promoted IDs
            List<UUID> promotedSQLIds = promoteDraftOffersToSQL(ingestionReportId);

            // 3. REPLACEMENT logic for both databases
            replaceOffers(dumpType, inventoryId, promotedNoSQLIds, promotedSQLIds);

            // 4. Process explicit deletions (offersToDelete from JSON) in both databases
            deleteOffersInPromotion(inventoryId, externalIdsToDelete);
    }

    private List<UUID> promoteDraftOffersAndGetsPromoted(UUID ingestionReportId, UUID inventoryId) {
        // 1. Use a Stream to avoid loading the entire list into memory and prevent OOM
        Query draftQuery = new Query(Criteria.where("ingestion_report_id").is(ingestionReportId));

        // List to track processed IDs for the REPLACEMENT logic
        List<UUID> promotedIds = new ArrayList<>();

        // 2. Define batch size for Bulk operations (e.g., every 100 records) for better performance
        int batchSize = 100;
        int count = 0;
        int totalPromoted = 0;
        int totalErrors = 0;

        // Use mongoTemplate.stream to open a cursor and process records one by one
        try (Stream<DraftOfferNoSQLEntity> draftStream = mongoTemplate.stream(draftQuery, DraftOfferNoSQLEntity.class)) {

            // Initialize the first bulk operation using UNORDERED mode for maximum speed
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, VehicleOfferNoSQLEntity.class);

            var iterator = draftStream.iterator();
            while (iterator.hasNext()) {
                try {
                    DraftOfferNoSQLEntity draft = iterator.next();

                    log.info("Processing draft offer - ID: {}, ownerRef: {}, dealerRef: {}, channelRef: {}",
                            draft.getId(), draft.getOwnerReference(), draft.getDealerReference(), draft.getChannelReference());

                    VehicleOfferNoSQLEntity productionEntity = offerInfraNoSQLMapper.toVehicleOfferNoSQLEntity(draft);
                    promotedIds.add(productionEntity.getId());

                    // 3. Build the query to find existing record
                    // Match by inventory_id AND all present business references (AND logic)
                    List<Criteria> andCriteria = new ArrayList<>();
                    andCriteria.add(Criteria.where("inventory_id").is(inventoryId));

                    boolean hasAnyReference = false;

                    // Handle Owner Reference: match exact value or ensure it's null/doesn't exist
                    if (draft.getOwnerReference() != null && !draft.getOwnerReference().isEmpty()) {
                        andCriteria.add(Criteria.where("owner_reference").is(draft.getOwnerReference()));
                        hasAnyReference = true;
                    } else {
                        andCriteria.add(new Criteria().orOperator(
                                Criteria.where("owner_reference").is(null),
                                Criteria.where("owner_reference").exists(false)
                        ));
                    }

                    // Handle Dealer Reference: match exact value or ensure it's null/doesn't exist
                    if (draft.getDealerReference() != null && !draft.getDealerReference().isEmpty()) {
                        andCriteria.add(Criteria.where("dealer_reference").is(draft.getDealerReference()));
                        hasAnyReference = true;
                    } else {
                        andCriteria.add(new Criteria().orOperator(
                                Criteria.where("dealer_reference").is(null),
                                Criteria.where("dealer_reference").exists(false)
                        ));
                    }

                    // Handle Channel Reference: match exact value or ensure it's null/doesn't exist
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

                    // If business references exist, use composite AND criteria. Else, fallback to ID.
                    if (hasAnyReference) {
                        upsertQuery = new Query(new Criteria().andOperator(andCriteria.toArray(new Criteria[0])));
                    } else {
                        upsertQuery = new Query(Criteria.where("_id").is(productionEntity.getId()));
                    }

                    // 4. Prepare the Update object using $set for all fields
                    Document doc = new Document();
                    mongoTemplate.getConverter().write(productionEntity, doc);

                    // IMPORTANT: Remove _id and _class to prevent immutable field errors in MongoDB
                    doc.remove("_id");
                    doc.remove("_class");

                    Update update = new Update();
                    for (String key : doc.keySet()) {
                        Object value = doc.get(key);
                        if (value != null) {
                            update.set(key, value);
                        }
                    }

                    // Add to bulk execution plan
                    bulkOps.upsert(upsertQuery, update);
                    count++;

                    // Execute batch when reaching chunk size
                    if (count >= batchSize) {
                        log.info("Executing bulk of {} operations", count);
                        var result = bulkOps.execute();
                        totalPromoted += (result.getInsertedCount() + result.getModifiedCount() + result.getUpserts().size());

                        // Reset for next batch
                        bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, VehicleOfferNoSQLEntity.class);
                        count = 0;
                    }
                } catch (Exception e) {
                    totalErrors++;
                    log.error("Error processing individual draft for promotion", e);
                    // We don't throw here to allow other records in the stream to finish
                }
            }

            // 5. Execute any remaining operations in the last batch
            if (count > 0) {
                log.debug("Executing final bulk of {} operations", count);
                var result = bulkOps.execute();
                totalPromoted += (result.getInsertedCount() + result.getModifiedCount() + result.getUpserts().size());
            }

            log.info("Promotion finished for report {}. Success: {}, Errors: {}", ingestionReportId, totalPromoted, totalErrors);

            // 6. Final Validation: If we have errors and zero success, we must inform the Application Service
            if (totalErrors > 0 && totalPromoted == 0) {
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.DATABASE)
                        .message("NoSQL promotion failed: All records failed for report " + ingestionReportId)
                        .build();
            }

        } catch (Exception e) {
            log.error("Critical failure during NoSQL promotion stream", e);
            if (e instanceof LCIngestionException){
                throw e;
            }
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error during NoSQL promotion for report: " + ingestionReportId)
                    .cause(e)
                    .build();
        }
        return promotedIds;
    }


    private List<UUID> promoteDraftOffersToSQL(UUID ingestionReportId) {
        log.info("Starting SQL promotion for ingestionReportId: {}", ingestionReportId);

        Query draftQuery = new Query(Criteria.where("ingestion_report_id").is(ingestionReportId));

        int batchSize = 50; // Process 50 offers per SQL transaction
        int totalProcessed = 0;
        int totalErrors = 0;
        List<UUID> allPromotedIds = new ArrayList<>();

        try (Stream<DraftOfferNoSQLEntity> draftStream = mongoTemplate.stream(draftQuery, DraftOfferNoSQLEntity.class)) {

            List<OfferDto> batch = new ArrayList<>();
            var iterator = draftStream.iterator();

            while (iterator.hasNext()) {
                DraftOfferNoSQLEntity draft = iterator.next();
                OfferDto offerDto = offerInfraNoSQLMapper.toDto(draft);
                batch.add(offerDto);

                // When batch is full or stream ends, process the entire batch
                if (batch.size() >= batchSize || !iterator.hasNext()) {
                    try {
                        // Process entire batch in ONE SQL transaction
                        List<UUID> batchPromotedIds = processBatchToSQL(new ArrayList<>(batch));
                        allPromotedIds.addAll(batchPromotedIds);
                        totalProcessed += batchPromotedIds.size();
                        log.info("Processed SQL batch of {} offers. Total: {}", batchPromotedIds.size(), totalProcessed);

                    } catch (Exception e) {
                        log.error("Batch offer failed", e);
                        totalErrors++;
                    }

                    batch.clear();
                }
            }

            log.info("SQL promotion completed. Processed: {}, Errors: {}, Total IDs: {}",
                    totalProcessed, totalErrors, allPromotedIds.size());

            if (totalErrors > 0 && totalProcessed == 0) {
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.DATABASE)
                        .message("Error in promotion all offers are not processed: " + ingestionReportId)
                        .build();
            }

            return allPromotedIds;

        } catch (Exception e) {
            if(e instanceof LCIngestionException){
                throw e;
            }
            log.error("Failed during SQL promotion", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error during SQL promotion for ingestionReportId: " + ingestionReportId)
                    .cause(e)
                    .build();
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    private List<UUID> processBatchToSQL(List<OfferDto> offers) {
        // This entire method runs in ONE SQL transaction
        // All offers in the batch are committed together

        List<UUID> processedIds = new ArrayList<>();

        for (OfferDto offer : offers) {
            try {
                offerInfraSQLService.processOffer(offer);
                processedIds.add(offer.getId());
            } catch (Exception e) {
                log.error("Error processing offer {} in batch", offer.getId(), e);
                // Re-throw to rollback entire batch
                if(e instanceof LCIngestionException){
                    throw e;
                }
                log.error("Failed during SQL promotion", e);
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.DATABASE)
                        .message("Error processing batch")
                        .cause(e)
                        .build();
            }
        }

        return processedIds;
    }

    private void replaceOffers(IngestionDumpType dumpType, UUID inventoryId,
                               List<UUID> promotedNoSQLIds, List<UUID> promotedSQLIds) {
        if (dumpType == IngestionDumpType.REPLACEMENT) {
            log.info("Executing REPLACEMENT cleanup for inventoryId {}", inventoryId);

            // Delete from NoSQL
            long noSqlDeleted = vehicleOfferNoSqlRepository.deleteByInventoryIdAndIdNotIn(inventoryId, promotedNoSQLIds);
            log.info("REPLACEMENT: Deleted {} offers from NoSQL vehicleoffers", noSqlDeleted);

            // Delete from SQL
            long sqlDeleted = replaceOffersInSQL(inventoryId, promotedSQLIds);
            log.info("REPLACEMENT: Deleted {} offers from SQL", sqlDeleted);
        }
    }

    @Transactional
    private long replaceOffersInSQL(UUID inventoryId, List<UUID> promotedIds) {
        // Delete offers in SQL for this inventory that were NOT promoted in this job

        if (promotedIds.isEmpty()) {
            // If no offers were promoted, delete all for this inventory
            long deleted = offerInfraSQLService.deleteOffersByInventoryId(inventoryId);
            log.info("No promoted IDs - deleted all {} offers for inventoryId {}", deleted, inventoryId);
            return deleted;
        }

        // Delete where inventory_id matches but id is NOT in the promoted list
        return offerInfraSQLService.deleteOffersByInventoryIdExcludingIds(inventoryId, promotedIds);
    }


    private void deleteOffersInPromotion(UUID inventoryId, List<String> externalIdsToDelete) {
        if (externalIdsToDelete != null && !externalIdsToDelete.isEmpty()) {
            log.info("Processing {} explicit deletions for inventory {}", externalIdsToDelete.size(), inventoryId);

            // Delete from NoSQL
            deleteOffersInPromotionNoSQL(inventoryId, externalIdsToDelete);

            // Delete from SQL
            deleteOffersInPromotionSQL(inventoryId, externalIdsToDelete);
        }
    }

    private void deleteOffersInPromotionNoSQL(UUID inventoryId, List<String> externalIdsToDelete) {
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
        log.info("Explicitly deleted {} offers from NoSQL production", deletedCount);
    }

    @Transactional
    private void deleteOffersInPromotionSQL(UUID inventoryId, List<String> externalIdsToDelete) {
        // Delete from SQL where ANY of the three references match
        // Using native query for better performance with OR conditions

        long deletedCount = offerInfraSQLService.deleteOffersByInventoryIdAndReferences(
                inventoryId,
                externalIdsToDelete
        );

        log.info("Explicitly deleted {} offers from SQL production", deletedCount);
    }

    @Override
    public void deleteDraftOffersByIngestionReportId(UUID ingestionReportId) {
        log.info("Starting delete offers by ingestionReportId: {}", ingestionReportId);
        long deletedCount = draftOfferNoSqlRepository.deleteByIngestionReportId(ingestionReportId);
        log.info("Deleted: {} offers with ingestionReportId: {}", deletedCount, ingestionReportId);
    }


}
