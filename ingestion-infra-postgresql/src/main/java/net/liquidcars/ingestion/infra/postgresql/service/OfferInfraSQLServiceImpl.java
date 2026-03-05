package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import net.liquidcars.ingestion.infra.postgresql.repository.*;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.liquidcars.ingestion.domain.service.utils.OfferUtils.buildCompositeKey;
import static net.liquidcars.ingestion.domain.service.utils.OfferUtils.extractRef;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {

    private final OfferSQLRepository offerSqlRepository;
    private final VehicleModelSQLRepository vehicleModelRepository;
    private final CarOfferResourceRepository carOfferResourceRepository;
    private final ParticipantAddressEntityRepository participantAddressEntityRepository;
    private final CarInstanceEquipmentEntityRepository carInstanceEquipmentEntityRepository;
    private final OfferInfraSQLMapper mapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final VehicleInstanceRepository vehicleInstanceRepository;
    private final AgreementRepository agreementRepository;

    @Transactional
    @Override
    public long deleteOffersByInventoryId(UUID inventoryId) {
        log.info("Starting full deletion for inventoryId: {}", inventoryId);
        try {
            offerSqlRepository.deleteCarloanPreviewByInventoryId(inventoryId);
            offerSqlRepository.deletePreordersCartByInventoryId(inventoryId);
            offerSqlRepository.deleteTinyLocatorsByInventoryId(inventoryId);
            offerSqlRepository.deleteResourcesByInventoryId(inventoryId);
            return offerSqlRepository.deleteMainOfferData(inventoryId);
        } catch (Exception e) {
            throw handleDeletionError(inventoryId, e);
        }
    }

    @Transactional
    @Override
    public long deleteOffersByInventoryIdExcludingIds(UUID inventoryId, List<UUID> idsToKeep) {
        log.info("Starting delta deletion for inventoryId: {} (keeping {} offers)", inventoryId, idsToKeep.size());
        try {
            if (idsToKeep.isEmpty()) return deleteOffersByInventoryId(inventoryId);

            offerSqlRepository.deleteCarloanPreviewByInventoryExcluding(inventoryId, idsToKeep);
            offerSqlRepository.deletePreordersCartByInventoryExcluding(inventoryId, idsToKeep);
            offerSqlRepository.deleteTinyLocatorsByInventoryExcluding(inventoryId, idsToKeep);
            offerSqlRepository.deleteResourcesByInventoryExcluding(inventoryId, idsToKeep);
            return offerSqlRepository.deleteMainOfferDataExcluding(inventoryId, idsToKeep);
        } catch (Exception e) {
            throw handleDeletionError(inventoryId, e);
        }
    }

    @Transactional
    @Override
    public long deleteOffersByInventoryIdAndReferences(UUID inventoryId, List<String> externalReferences) {
        log.info("Starting explicit reference deletion for inventoryId: {}", inventoryId);
        try {
            if (externalReferences.isEmpty()) return 0;

            offerSqlRepository.deleteCarloanPreviewByReferences(inventoryId, externalReferences);
            offerSqlRepository.deletePreordersCartByReferences(inventoryId, externalReferences);
            offerSqlRepository.deleteTinyLocatorsByReferences(inventoryId, externalReferences);
            offerSqlRepository.deleteResourcesByReferences(inventoryId, externalReferences);
            return offerSqlRepository.deleteMainOfferDataByRefs(inventoryId, externalReferences);
        } catch (Exception e) {
            throw handleDeletionError(inventoryId, e);
        }
    }

    private RuntimeException handleDeletionError(UUID inventoryId, Exception e) {
        log.error("SQL deletion error for inventoryId: {}", inventoryId, e);
        return LCIngestionException.builder()
                .techCause(LCTechCauseEnum.DATABASE)
                .message("Database error during offer deletion")
                .cause(e)
                .build();
    }


    private CarOfferResourceEntity buildCarOfferResourceEntity(CarOfferResourceDto dto, OfferEntity offer) {
        ResourceTypeEntity type = ResourceTypeEntity.builder()
                .id("UrlImage")
                .build();
        byte[] image = convertUrlToBytes(dto.getResource());
        return CarOfferResourceEntity.builder()
                .id(null)
                .offer(offer)
                .resourceType(type)
                .resource(image)
                .build();
    }

    private byte[] convertUrlToBytes(String url) {
        if (url == null) return null;
        return url.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private JsonOfferEntity buildJsonEntity(OfferDto offer) {
        JsonObjectTypeEntity type = JsonObjectTypeEntity.builder()
                .id("caroffer")
                .build();
        JsonObjectEntity baseObject = JsonObjectEntity.builder()
                .id(UUID.randomUUID())
                .jsonObjectType(type)
                .createdAt(OffsetDateTime.now())
                .build();
        Map<String, Object> jsonMap = objectMapper.convertValue(
                offer.getUICarOffer(),
                java.util.Map.class
        );
        JsonOfferEntity jsonEntity = JsonOfferEntity.builder()
                .jsonObject(baseObject)
                .jsonOfferClass("UIOffer")
                .texto(jsonMap)
                .createdAt(OffsetDateTime.now())
                .build();
        return jsonEntity;
    }

    private VehicleModelEntity ensureVehicleModelExists(VehicleModelDto dto) {
        return vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(
                        dto.getBrand(), dto.getModel(), dto.getVersion())
                .orElseGet(() -> {
                    VehicleModelEntity model = mapper.toVehicleModelEntity(dto);
                    model.setId(null);
                    model.setEnabled(true);
                    return vehicleModelRepository.save(model);
                });
    }

    private VehicleInstanceEntity ensureVehicleInstanceExists(VehicleInstanceDto dto, VehicleModelEntity model) {
        String plate = dto.getPlate();
        String chassis = dto.getChassisNumber();
        return vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(plate, chassis)
                .orElseGet(() -> {
                    log.info("Vehicle not found, creating: {}", plate);
                    VehicleInstanceEntity entity = mapper.toVehicleInstanceEntity(dto);
                    entity.setId(null);
                    entity.setPlate(plate);
                    entity.setChassisNumber(chassis);
                    entity.setEnabled(true);
                    entity.setVehicleModel(model);
                    return vehicleInstanceRepository.saveAndFlush(entity);
                });
    }

    /**
     * Optimized batch processing for promotion flow.
     * Instead of N individual transactions (one per offer), this method:
     * 1. Loads all existing offers in ONE query
     * 2. Classifies them into inserts/updates
     * 3. Persists everything in ONE transaction using saveAll (Hibernate batch)
     * 4. Handles resources, equipments and addresses in batch too
     *
     * Offers with active bookings are protected: they are added to processedIds
     * (so REPLACEMENT doesn't delete them) but their data is NOT updated.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> processBatch(List<OfferDto> offers, List<UUID> activeBookedOfferIds) {
        if (offers.isEmpty()) return List.of();

        UUID inventoryId = offers.get(0).getInventoryId();
        log.info("Processing SQL batch of {} offers for inventoryId: {}", offers.size(), inventoryId);

        try {
            // 1. Build vehicle model cache to avoid N queries to vehicle_model table
            Map<String, VehicleModelEntity> modelCache = buildModelCache(offers);
            Map<String, VehicleInstanceEntity> instanceCache = buildVehicleInstanceCache(offers, modelCache);

            // 2. Resolve booked refs upfront to protect offers with active bookings
            Set<String> bookedRefs = activeBookedOfferIds.isEmpty()
                    ? Set.of()
                    : findExternalRefsByOfferIds(activeBookedOfferIds);

            // 3. Load all existing offers in ONE query instead of one per offer
            Map<String, OfferEntity> existingByRef = loadExistingOffers(inventoryId, offers);
            Map<UUID, OfferEntity> existingById = existingByRef.values().stream()
                    .collect(Collectors.toMap(OfferEntity::getId, e -> e, (a, b) -> a));

            List<OfferEntity> toInsert = new ArrayList<>();
            List<OfferEntity> toUpdate = new ArrayList<>();
            List<OfferDto> insertDtos = new ArrayList<>(); // Parallel list for resources/equipments post-save
            List<OfferDto> updateDtos = new ArrayList<>();
            List<UUID> processedIds = new ArrayList<>();
            Set<UUID> insertedIdsInThisBatch = new HashSet<>(); // añadir antes del for

            for (OfferDto offer : offers) {
                try {
                    String ref = extractRef(offer.getExternalIdInfo());
                    VehicleModelEntity model = modelCache.get(modelKeyModel(offer.getVehicleInstance().getVehicleModel()));
                    VehicleInstanceEntity instance = instanceCache.get(modelKeyInstance(offer.getVehicleInstance()));
                    OfferEntity existing = existingByRef.get(ref);

                    if (existing == null) {
                        existing = existingById.get(offer.getId());
                    }

                    if (existing != null) {
                        // BOOKING PROTECTION: if the offer has an active booking, skip update
                        // but include its ID in processedIds so REPLACEMENT does not delete it
                        if (bookedRefs.contains(ref)) {
                            log.info("Skipping SQL update for booked offer ref: {}", ref);
                            processedIds.add(existing.getId());
                            continue;
                        }

                        // Update logic
                        OffsetDateTime incomingDate = mapper.mapEpoch(offer.getLastUpdated());
                        OffsetDateTime existingDate = existing.getLastUpdated() != null
                                ? existing.getLastUpdated() : existing.getCreatedAt();

                        if (incomingDate.isAfter(existingDate)) {
                            mapper.updateEntityFromDto(offer, existing);
                            existing.setLastUpdated(incomingDate);

                            if (existing.getVehicleInstance() != null) {
                                mapper.updateVehicleInstanceFromDto(offer.getVehicleInstance(), instance);
                                instance.setVehicleModel(model);
                                existing.setVehicleInstance(instance);
                            }

                            if (existing.getJsonCarOffer() != null) {
                                existing.getJsonCarOffer().setTexto(
                                        objectMapper.convertValue(offer.getUICarOffer(), Map.class)
                                );
                                existing.getJsonCarOffer().setCreatedAt(OffsetDateTime.now());
                            }
                            toUpdate.add(existing);
                            updateDtos.add(offer);
                        }
                        processedIds.add(existing.getId());

                    } else {
                        if (offer.getId() != null && existingById.containsKey(offer.getId())) {
                            log.warn("Offer {} not found by ref but UUID exists in DB — treating as update", offer.getId());
                            OfferEntity byIdFallback = existingById.get(offer.getId());
                            toUpdate.add(byIdFallback);
                            updateDtos.add(offer);
                            processedIds.add(byIdFallback.getId());
                        } else {
                            if (insertedIdsInThisBatch.contains(offer.getId())) {
                                log.warn("Duplicate offer in batch (same derived UUID {}), skipping: ref={}", offer.getId(), ref);
                                processedIds.add(offer.getId());
                                continue;
                            }
                            insertedIdsInThisBatch.add(offer.getId());
                            // Create logic - new offers are always inserted, even if a booking exists for a new ref
                            OfferEntity newEntity = mapper.toEntity(offer);
                            mapper.updateVehicleInstanceFromDto(offer.getVehicleInstance(), instance);
                            instance.setVehicleModel(model);
                            newEntity.setVehicleInstance(instance);
                            newEntity.setJsonCarOffer(buildJsonEntity(offer));
                            toInsert.add(newEntity);
                            insertDtos.add(offer);
                            processedIds.add(offer.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error preparing offer {} in batch", offer.getId(), e);
                }
            }

            // 4. Persist all inserts and updates in ONE flush (Hibernate batch_size from config applies here)
            List<OfferEntity> savedInserts = offerSqlRepository.saveAll(toInsert);
            offerSqlRepository.flush();
            offerSqlRepository.saveAll(toUpdate);

            // 5. Resources in batch (delete all for affected offers, then re-insert)
            saveResourcesBatch(savedInserts, insertDtos);
            saveResourcesBatch(toUpdate, updateDtos);

            // 6. Equipments in batch
            saveEquipmentsBatch(savedInserts, insertDtos);
            saveEquipmentsBatch(toUpdate, updateDtos);

            // 7. Addresses in batch
            saveAddressesBatch(offers);

            log.info("SQL batch completed for inventoryId: {}. Inserts: {}, Updates: {}, ProcessedIds: {}",
                    inventoryId, toInsert.size(), toUpdate.size(), processedIds.size());

            return processedIds;

        } catch (Exception e) {
            log.error("Batch processing failed for inventoryId: {}", inventoryId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error processing SQL batch for inventoryId: " + inventoryId)
                    .cause(e)
                    .build();
        }
    }


// --- Métodos de soporte batch ---

    private Map<String, VehicleModelEntity> buildModelCache(List<OfferDto> offers) {
        Map<String, VehicleModelEntity> cache = new HashMap<>();
        for (OfferDto offer : offers) {
            VehicleModelDto dto = offer.getVehicleInstance().getVehicleModel();
            String key = modelKeyModel(dto);
            cache.computeIfAbsent(key, k -> ensureVehicleModelExists(dto));
        }
        return cache;
    }

    private String modelKeyModel(VehicleModelDto dto) {
        return (dto.getBrand() + "|" + dto.getModel() + "|" + dto.getVersion()).toLowerCase();
    }

    private Map<String, VehicleInstanceEntity> buildVehicleInstanceCache(
            List<OfferDto> offers,
            Map<String, VehicleModelEntity> modelCache) {

        Map<String, VehicleInstanceEntity> cache = new HashMap<>();
        for (OfferDto offer : offers) {
            VehicleInstanceDto dto = offer.getVehicleInstance();
            String key = modelKeyInstance(dto);
            if (!cache.containsKey(key)) {
                VehicleModelEntity model = modelCache.get(modelKeyModel(dto.getVehicleModel()));
                cache.put(key, ensureVehicleInstanceExists(dto, model));
            }
        }
        return cache;
    }

    private String modelKeyInstance(VehicleInstanceDto dto) {
        return (dto.getPlate() + "|" + dto.getChassisNumber()).toLowerCase();
    }

    private Map<String, OfferEntity> loadExistingOffers(UUID inventoryId, List<OfferDto> offers) {
        List<String> owners = offers.stream().map(o -> o.getExternalIdInfo().getOwnerReference()).filter(Objects::nonNull).toList();
        List<String> dealers = offers.stream().map(o -> o.getExternalIdInfo().getDealerReference()).filter(Objects::nonNull).toList();
        List<String> channels = offers.stream().map(o -> o.getExternalIdInfo().getChannelReference()).filter(Objects::nonNull).toList();

        List<OfferEntity> byRef = (owners.isEmpty() && dealers.isEmpty() && channels.isEmpty())
                ? List.of()
                : offerSqlRepository.findExistingByAnyRef(inventoryId, owners, dealers, channels);

        List<UUID> incomingIds = offers.stream().map(OfferDto::getId).filter(Objects::nonNull).toList();

        List<OfferEntity> byId = incomingIds.isEmpty()
                ? List.of()
                : offerSqlRepository.findAllById(incomingIds);

        Map<String, OfferEntity> result = new HashMap<>();

        byId.forEach(e -> result.put(
                buildCompositeKey(e.getOwnerReference(), e.getDealerReference(), e.getChannelReference()), e
        ));

        byRef.forEach(e -> result.put(
                buildCompositeKey(e.getOwnerReference(), e.getDealerReference(), e.getChannelReference()), e
        ));

        return result;
    }

    private void saveResourcesBatch(List<OfferEntity> offers, List<OfferDto> dtos) {
        if (offers.isEmpty()) return;

        // Borrar todos los recursos de estas ofertas en UNA query
        List<UUID> offerIds = offers.stream().map(OfferEntity::getId).toList();
        offerSqlRepository.deleteByOfferIdIn(offerIds);

        // Construir todos los recursos de golpe
        List<CarOfferResourceEntity> allResources = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            OfferDto dto = dtos.get(i);
            OfferEntity entity = offers.get(i);
            if (dto.getResources() != null) {
                dto.getResources().forEach(r -> allResources.add(buildCarOfferResourceEntity(r, entity)));
            }
        }
        if (!allResources.isEmpty()) {
            carOfferResourceRepository.saveAll(allResources);
        }
    }

    private void saveEquipmentsBatch(List<OfferEntity> offers, List<OfferDto> dtos) {
        if (offers.isEmpty()) return;

        List<Long> vehicleInstanceIds = offers.stream()
                .filter(o -> o.getVehicleInstance() != null)
                .map(o -> o.getVehicleInstance().getId())
                .toList();

        offerSqlRepository.deleteByVehicleInstanceIdIn(vehicleInstanceIds);

        List<CarInstanceEquipmentEntity> allEquipments = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            OfferDto dto = dtos.get(i);
            OfferEntity entity = offers.get(i);
            if (dto.getVehicleInstance().getEquipments() != null && entity.getVehicleInstance() != null) {
                List<CarInstanceEquipmentEntity> entities = mapper.toCarInstanceEquipmentEntityList(
                        dto.getVehicleInstance().getEquipments()
                );
                entities.forEach(eq -> {
                    eq.setVehicleInstance(entity.getVehicleInstance());
                    if (eq.getType() == null) {
                        eq.setType(EquipmentTypeEntity.builder().id("Other").build());
                    }
                });
                allEquipments.addAll(entities);
            }
        }
        if (!allEquipments.isEmpty()) {
            carInstanceEquipmentEntityRepository.saveAll(allEquipments);
        }
    }

    private void saveAddressesBatch(List<OfferDto> offers) {
        List<ParticipantAddressEntity> toSave = new ArrayList<>();
        for (OfferDto offer : offers) {
            if (offer.getPickUpAddress() != null && offer.getParticipantId() != null) {
                ParticipantAddressEntity addr = mapper.toParticipantAddressEntity(offer.getPickUpAddress());
                addr.setId(offer.getParticipantId());
                toSave.add(addr);
            }
        }
        if (!toSave.isEmpty()) {
            participantAddressEntityRepository.saveAll(toSave);
        }
    }

    @Override
    public List<UUID> findActiveBookedOfferIds(UUID inventoryId){
        log.info("Find active bookings for inventoryId: {}", inventoryId);
        try {
            return offerSqlRepository.findActiveBookedOfferIds(inventoryId);
        } catch (Exception e) {
            log.error("Error finding active bookings for inventoryId: {}", inventoryId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error finding active bookings for inventoryId: " + inventoryId)
                    .cause(e)
                    .build();
        }
    }

    @Override
    public Set<String> findExternalRefsByOfferIds(List<UUID> offerIds) {
        log.info("Find external references for offer ids: {}", offerIds);
        try {
            if (offerIds.isEmpty()) return Set.of();
            return offerSqlRepository.findExternalRefsByOfferIds(offerIds)
                    .stream()
                    .flatMap(e -> Stream.of(e.getOwnerReference(), e.getDealerReference(), e.getChannelReference()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Error finding external references for offer ids: {}", offerIds, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error finding external references for offer ids: " + offerIds)
                    .cause(e)
                    .build();
        }
    }

    @Override
    public List<AgreementDto> findAgreementsByInventoryId(UUID inventoryId){
        log.info("Find agreements by inventoryID: {}", inventoryId);
        try {
            return mapper.toAgreementDtoList(agreementRepository.findByInventoryId(inventoryId));
        } catch (Exception e) {
            log.error("Error finding agreements by inventoryId: {}", inventoryId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Error finding agreements by inventoryId: " + inventoryId)
                    .cause(e)
                    .build();
        }
    }

}
