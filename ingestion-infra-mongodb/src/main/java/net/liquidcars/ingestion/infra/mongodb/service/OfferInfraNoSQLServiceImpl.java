package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.SortDirection;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportFilterDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportSortField;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IReportInfraSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.VehicleOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.DraftOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.repository.VehicleOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.bson.Document;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
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

    private final IReportInfraSQLService reportInfraSQLService;

    @Override
    @Transactional
    public void processOffer(OfferDto offer) {
        log.info("Processing NoSQL persistence for id: {}", offer.getId());

        try {
            DraftOfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);
            entity.setCreatedAt(Instant.now());
            findByExternalIdentities(offer.getInventoryId(), offer.getExternalIdInfo())
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

    private Optional<DraftOfferNoSQLEntity> findByExternalIdentities(
            UUID inventoryId, ExternalIdInfoDto externalIdInfoDto) {

        List<Criteria> andCriteria = new ArrayList<>();
        andCriteria.add(Criteria.where("inventory_id").is(inventoryId));
        if(externalIdInfoDto!=null) {
            if (externalIdInfoDto.getOwnerReference() != null) andCriteria.add(Criteria.where("owner_reference").is(externalIdInfoDto.getOwnerReference()));
            if (externalIdInfoDto.getDealerReference() != null) andCriteria.add(Criteria.where("dealer_reference").is(externalIdInfoDto.getDealerReference()));
            if (externalIdInfoDto.getChannelReference() != null) andCriteria.add(Criteria.where("channel_reference").is(externalIdInfoDto.getChannelReference()));
        }

        Query query = new Query(new Criteria().andOperator(andCriteria.toArray(new Criteria[0])));

        return Optional.ofNullable(mongoTemplate.findOne(query, DraftOfferNoSQLEntity.class));
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
    public void promoteDraftOffersToVehicleOffers(UUID ingestionReportId, IngestionDumpType dumpType, UUID inventoryId, List<String> externalIdsToDelete, List<UUID> activeBookedOfferIds) {
        log.info("Starting promotion for ingestionReportId: {}", ingestionReportId);

        // 1. Promote to NoSQL (vehicle offers collection)
        List<UUID> promotedNoSQLIds = promoteDraftOffersAndGetsPromoted(ingestionReportId, inventoryId, activeBookedOfferIds);

        // 2. Promote to SQL (PostgreSQL) and get promoted IDs
        List<UUID> promotedSQLIds = promoteDraftOffersToSQL(ingestionReportId, activeBookedOfferIds);

        // 3. REPLACEMENT logic for both databases
        replaceOffers(dumpType, inventoryId, promotedNoSQLIds, promotedSQLIds, activeBookedOfferIds);

        // 4. Process explicit deletions (offersToDelete from JSON) in both databases
        deleteOffersInPromotion(inventoryId, externalIdsToDelete, activeBookedOfferIds);
    }

    private List<UUID> promoteDraftOffersAndGetsPromoted(UUID ingestionReportId, UUID inventoryId,
                                                         List<UUID> activeBookedOfferIds) {
        // 1. Use a Stream to avoid loading the entire list into memory and prevent OOM
        Query draftQuery = new Query(Criteria.where("ingestion_report_id").is(ingestionReportId));

        // Load external refs of booked offers upfront to avoid per-record SQL lookups during the stream
        Set<String> bookedRefs = activeBookedOfferIds.isEmpty()
                ? Set.of()
                : offerInfraSQLService.findExternalRefsByOfferIds(activeBookedOfferIds);

        // List to track processed IDs for the REPLACEMENT logic
        List<UUID> promotedIds = new ArrayList<>();

        // 2. Define batch size for Bulk operations (e.g., every 100 records) for better performance
        int batchSize = 100;
        int count = 0;
        int totalPromoted = 0;
        int totalErrors = 0;
        int totalSkipped = 0;

        Map<String, UUID> existingProductionIds = loadAllProductionIdsByInventory(inventoryId);

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

                    // BOOKING PROTECTION: if the offer has an active booking, skip the upsert
                    // but still add its production ID to promotedIds so REPLACEMENT does not delete it
                    if (isBooked(draft, bookedRefs)) {
                        log.info("Skipping NoSQL upsert for booked offer - ownerRef: {}", draft.getOwnerReference());
                        findProductionIdByRefs(inventoryId, draft).ifPresent(promotedIds::add);
                        totalSkipped++;
                        continue;
                    }

                    VehicleOfferNoSQLEntity productionEntity = offerInfraNoSQLMapper.toVehicleOfferNoSQLEntity(draft);

                    String ref = draft.getOwnerReference() != null ? draft.getOwnerReference()
                            : draft.getDealerReference() != null ? draft.getDealerReference()
                            : draft.getChannelReference();

                    UUID productionEntityId = existingProductionIds.getOrDefault(ref, productionEntity.getId());
                    promotedIds.add(productionEntityId);

                    // 3. Build the query to find existing record
                    // Match by inventory_id AND all present business references (AND logic)
                    List<Criteria> andCriteria = new ArrayList<>();
                    andCriteria.add(Criteria.where("inventory_id").is(inventoryId));
                    if (draft.getOwnerReference() != null) {
                        andCriteria.add(Criteria.where("owner_reference").is(draft.getOwnerReference()));
                    }
                    if (draft.getDealerReference() != null) {
                        andCriteria.add(Criteria.where("dealer_reference").is(draft.getDealerReference()));
                    }
                    if (draft.getChannelReference() != null) {
                        andCriteria.add(Criteria.where("channel_reference").is(draft.getChannelReference()));
                    }

                    Query upsertQuery = new Query(new Criteria().andOperator(andCriteria.toArray(new Criteria[0])));

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

                    // Always force inventory_id from parameter to guarantee it is never null
                    update.set("inventory_id", inventoryId);
                    // On INSERT: fix the UUID as _id so promotedIds tracking stays consistent
                    update.setOnInsert("_id", productionEntityId);


                    // Add to bulk execution plan
                    bulkOps.upsert(upsertQuery, update);
                    count++;

                    // Execute batch when reaching chunk size
                    if (count >= batchSize) {
                        log.info("Executing bulk of {} operations", count);
                        var result = bulkOps.execute();

                        totalPromoted += result.getModifiedCount() + result.getUpserts().size();

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
                totalPromoted += result.getModifiedCount() + result.getUpserts().size();
            }

            log.info("Promotion finished for report {}. Success: {}, Skipped (booked): {}, Errors: {}",
                    ingestionReportId, totalPromoted, totalSkipped, totalErrors);

            // 6. Final Validation: If we have errors and zero success, we must inform the Application Service
            if (totalErrors > 0 && totalPromoted == 0 && totalSkipped == 0) {
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.DATABASE)
                        .message("NoSQL promotion failed: All records failed for report " + ingestionReportId)
                        .build();
            }

        } catch (Exception e) {
            log.error("Critical failure during NoSQL promotion stream", e);
            if (e instanceof LCIngestionException) {
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

    private Map<String, UUID> loadAllProductionIdsByInventory(UUID inventoryId) {
        Query q = new Query(Criteria.where("inventory_id").is(inventoryId));
        q.fields().include("_id").include("owner_reference").include("dealer_reference").include("channel_reference");

        return mongoTemplate.find(q, VehicleOfferNoSQLEntity.class)
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getOwnerReference() != null ? e.getOwnerReference()
                                : e.getDealerReference() != null ? e.getDealerReference()
                                : e.getChannelReference(),
                        VehicleOfferNoSQLEntity::getId,
                        (a, b) -> a
                ));
    }

    /**
     * Returns true if any of the draft's external references matches a booked ref.
     * Used to skip upsert on offers with active bookings.
     */
    private boolean isBooked(DraftOfferNoSQLEntity draft, Set<String> bookedRefs) {
        if (bookedRefs.isEmpty()) return false;
        return (draft.getOwnerReference() != null && bookedRefs.contains(draft.getOwnerReference()))
                || (draft.getDealerReference() != null && bookedRefs.contains(draft.getDealerReference()))
                || (draft.getChannelReference() != null && bookedRefs.contains(draft.getChannelReference()));
    }

    /**
     * Looks up the existing production document _id for a booked offer so it can be added
     * to promotedIds and protected from REPLACEMENT deletion.
     */
    private Optional<UUID> findProductionIdByRefs(UUID inventoryId, DraftOfferNoSQLEntity draft) {
        List<Criteria> criteria = new ArrayList<>();
        criteria.add(Criteria.where("inventory_id").is(inventoryId));
        if (draft.getOwnerReference() != null) criteria.add(Criteria.where("owner_reference").is(draft.getOwnerReference()));
        if (draft.getDealerReference() != null) criteria.add(Criteria.where("dealer_reference").is(draft.getDealerReference()));
        if (draft.getChannelReference() != null) criteria.add(Criteria.where("channel_reference").is(draft.getChannelReference()));

        Query q = new Query(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        q.fields().include("_id"); // Only fetch the ID, no need for full document

        VehicleOfferNoSQLEntity existing = mongoTemplate.findOne(q, VehicleOfferNoSQLEntity.class);
        return Optional.ofNullable(existing).map(VehicleOfferNoSQLEntity::getId);
    }

    private List<UUID> promoteDraftOffersToSQL(UUID ingestionReportId, List<UUID> activeBookedOfferIds) {
        log.info("Starting SQL promotion for ingestionReportId: {}", ingestionReportId);

        Query draftQuery = new Query(Criteria.where("ingestion_report_id").is(ingestionReportId));

        int batchSize = 100; // Process 100 offers per SQL transaction
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
                        List<UUID> batchPromotedIds = processBatchToSQL(new ArrayList<>(batch), activeBookedOfferIds);
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
            if (e instanceof LCIngestionException) {
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

    private List<UUID> processBatchToSQL(List<OfferDto> offers, List<UUID> activeBookedOfferIds) {
        try {
            return offerInfraSQLService.processBatch(offers, activeBookedOfferIds);
        } catch (Exception e) {
            log.error("Batch failed", e);
            throw e;
        }
    }

    private void replaceOffers(IngestionDumpType dumpType, UUID inventoryId,
                               List<UUID> promotedNoSQLIds, List<UUID> promotedSQLIds,
                               List<UUID> activeBookedOfferIds) {
        if (dumpType == IngestionDumpType.REPLACEMENT && !promotedNoSQLIds.isEmpty() && !promotedSQLIds.isEmpty()) {
            log.info("Executing REPLACEMENT cleanup for inventoryId {}", inventoryId);

            // Combine promoted + booked = everything that must NOT be deleted
            List<UUID> noSQLIdsToKeep = new ArrayList<>(promotedNoSQLIds);
            noSQLIdsToKeep.addAll(activeBookedOfferIds);

            List<UUID> sqlIdsToKeep = new ArrayList<>(promotedSQLIds);
            sqlIdsToKeep.addAll(activeBookedOfferIds);

            // Delete from NoSQL
            long noSqlDeleted = vehicleOfferNoSqlRepository.deleteByInventoryIdAndIdNotIn(inventoryId, noSQLIdsToKeep);
            log.info("REPLACEMENT: Deleted {} offers from NoSQL vehicleoffers (protected {} booked)",
                    noSqlDeleted, activeBookedOfferIds.size());

            // Delete from SQL
            long sqlDeleted = replaceOffersInSQL(inventoryId, sqlIdsToKeep);
            log.info("REPLACEMENT: Deleted {} offers from SQL (protected {} booked)",
                    sqlDeleted, activeBookedOfferIds.size());
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

    private void deleteOffersInPromotion(UUID inventoryId, List<String> externalIdsToDelete,
                                         List<UUID> activeBookedOfferIds) {
        if (externalIdsToDelete == null || externalIdsToDelete.isEmpty()) return;

        log.info("Processing {} explicit deletions for inventory {}", externalIdsToDelete.size(), inventoryId);

        // If no active bookings, proceed normally without filtering
        if (activeBookedOfferIds.isEmpty()) {
            deleteOffersInPromotionNoSQL(inventoryId, externalIdsToDelete);
            deleteOffersInPromotionSQL(inventoryId, externalIdsToDelete);
            return;
        }

        // Filter out refs that belong to booked offers to avoid cancelling active reservations
        Set<String> bookedRefs = offerInfraSQLService.findExternalRefsByOfferIds(activeBookedOfferIds);

        List<String> safeToDelete = externalIdsToDelete.stream()
                .filter(ref -> !bookedRefs.contains(ref))
                .toList();

        List<String> skipped = externalIdsToDelete.stream()
                .filter(bookedRefs::contains)
                .toList();

        if (!skipped.isEmpty()) {
            log.warn("Skipping explicit deletion of {} refs with active bookings: {}", skipped.size(), skipped);
        }

        if (!safeToDelete.isEmpty()) {
            log.info("Processing {} explicit deletions (filtered from {}) for inventory {}",
                    safeToDelete.size(), externalIdsToDelete.size(), inventoryId);

            // Delete from NoSQL
            deleteOffersInPromotionNoSQL(inventoryId, safeToDelete);

            // Delete from SQL
            deleteOffersInPromotionSQL(inventoryId, safeToDelete);
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
