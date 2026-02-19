package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.CarInstanceEquipmentDto;
import net.liquidcars.ingestion.domain.model.CarOfferResourceDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.VehicleModelDto;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    @Transactional
    @Override
    public long deleteOffersByInventoryId(UUID inventoryId) {
        log.info("Starting deletion of all offers for inventoryId: {}", inventoryId);
        try {
            long deletedCount = offerSqlRepository.deleteByInventoryId(inventoryId);
            log.info("Successfully deleted {} offers for inventoryId: {}", deletedCount, inventoryId);
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to delete offers for inventoryId: {}", inventoryId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL deletion error for inventoryId: " + inventoryId)
                    .cause(e)
                    .build();
        }
    }

    @Transactional
    @Override
    public long deleteOffersByInventoryIdExcludingIds(UUID inventoryId, List<UUID> idsToKeep) {
        log.info("Starting deletion of offers for inventoryId: {} excluding {} IDs",
                inventoryId, idsToKeep.size());
        try {
            if (idsToKeep.isEmpty()) {
                log.warn("No IDs to keep provided - deleting all offers for inventoryId: {}", inventoryId);
                return deleteOffersByInventoryId(inventoryId);
            }

            long deletedCount = offerSqlRepository.deleteByInventoryIdAndIdNotIn(inventoryId, idsToKeep);
            log.info("Successfully deleted {} offers for inventoryId: {} (kept {} offers)",
                    deletedCount, inventoryId, idsToKeep.size());
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to delete offers for inventoryId: {} excluding IDs", inventoryId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL deletion error for inventoryId: " + inventoryId + " excluding specific IDs")
                    .cause(e)
                    .build();
        }
    }

    @Transactional
    @Override
    public long deleteOffersByInventoryIdAndReferences(UUID inventoryId, List<String> externalReferences) {
        log.info("Starting deletion of {} offers by references for inventoryId: {}",
                externalReferences.size(), inventoryId);
        try {
            if (externalReferences.isEmpty()) {
                log.warn("No external references provided for deletion");
                return 0;
            }

            long deletedCount = offerSqlRepository.deleteByInventoryIdAndReferencesIn(
                    inventoryId,
                    externalReferences
            );
            log.info("Successfully deleted {} offers by references for inventoryId: {}",
                    deletedCount, inventoryId);
            return deletedCount;
        } catch (Exception e) {
            log.error("Failed to delete offers by references for inventoryId: {}", inventoryId, e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL deletion error for inventoryId: " + inventoryId + " by references")
                    .cause(e)
                    .build();
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOffer(OfferDto offer) {
        log.info("Processing SQL persistence for id: {}", offer.getId());
        try {
            // Creates model if it doesn't exist on bd
            VehicleModelEntity vehicleModelEntity = ensureVehicleModelExists(offer.getVehicleInstance().getVehicleModel());
            offerSqlRepository.findByHash(offer.getHash()).ifPresentOrElse(
                    existingEntity -> {
                        // Update logic
                        OffsetDateTime incomingDate = mapper.mapEpoch(offer.getLastUpdated());
                        OffsetDateTime existingDate = existingEntity.getLastUpdated() != null
                                ? existingEntity.getLastUpdated()
                                : existingEntity.getCreatedAt();

                        if (incomingDate.isAfter(existingDate)) {
                            log.info("Updating existing offer ID: {}", offer.getId());
                            updateFullOffer(existingEntity, offer, incomingDate, vehicleModelEntity);
                        }
                    },
                    // Create logic
                    () -> {
                        log.info("Creating new offer ID: {}", offer.getId());
                        createNewOffer(offer, vehicleModelEntity);
                    }
            );
        } catch (Exception e) {
            log.error("Failed to persist offer in SQL database. ID: {}", offer.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence error for id: " + offer.getId())
                    .cause(e)
                    .build();
        }
    }

    /**
     * Lógica de creación
     */
    private void createNewOffer(OfferDto offer, VehicleModelEntity vehicleModelEntity) {
        OfferEntity newEntity = mapper.toEntity(offer);
        if (newEntity.getVehicleInstance() != null) {
            newEntity.getVehicleInstance().setVehicleModel(vehicleModelEntity);
            if (newEntity.getVehicleInstance().getId() == null || newEntity.getVehicleInstance().getId() == 0) {
                newEntity.getVehicleInstance().setId(ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L));
            }
        }
        newEntity.setJsonCarOffer(buildJsonEntity(offer));
        OfferEntity savedOffer = offerSqlRepository.saveAndFlush(newEntity);
        if (offer.getPickUpAddress() != null) {
            ParticipantAddressEntity newAddress = mapper.toParticipantAddressEntity(offer.getPickUpAddress());
            newAddress.setId(offer.getParticipantId());
            participantAddressEntityRepository.save(newAddress);
        }
        saveOrUpdateResources(offer.getResources(), savedOffer);
        saveOrUpdateEquipments(offer.getVehicleInstance().getEquipments(), savedOffer.getVehicleInstance());

    }

    /**
     * Lógica de actualización
     */
    private void updateFullOffer(OfferEntity existing, OfferDto dto, OffsetDateTime updateDate, VehicleModelEntity correctModel) {
        // Save the existing vehicleInstance ID before mapping overwrites it
        Long existingVehicleInstanceId = existing.getVehicleInstance() != null
                ? existing.getVehicleInstance().getId()
                : null;

        mapper.updateEntityFromDto(dto, existing);
        existing.setLastUpdated(updateDate);

        // Restore the ID on the (possibly re-mapped) vehicleInstance
        if (existing.getVehicleInstance() != null) {
            existing.getVehicleInstance().setVehicleModel(correctModel);

            // 4. Restaurar el ID de la instancia de vehículo
            if (existingVehicleInstanceId != null) {
                existing.getVehicleInstance().setId(existingVehicleInstanceId);
            } else if (existing.getVehicleInstance().getId() == null) {
                existing.getVehicleInstance().setId(
                        ThreadLocalRandom.current().nextLong(100_000_000L, 999_999_999L)
                );
            }
        }

        if (existing.getJsonCarOffer() != null) {
            Map<String, Object> jsonMap = objectMapper.convertValue(dto.getUICarOffer(), Map.class);
            existing.getJsonCarOffer().setTexto(jsonMap);
            existing.getJsonCarOffer().setCreatedAt(OffsetDateTime.now());
        }
        offerSqlRepository.save(existing);
        if (dto.getPickUpAddress() != null) {
            participantAddressEntityRepository.findById(dto.getParticipantId())
                    .ifPresentOrElse(
                            addr -> mapper.updateAddressFromDto(dto.getPickUpAddress(), addr),
                            () -> {
                                ParticipantAddressEntity newAddr = mapper.toParticipantAddressEntity(dto.getPickUpAddress());
                                newAddr.setId(dto.getParticipantId());
                                participantAddressEntityRepository.save(newAddr);
                            }
                    );
        }
        saveOrUpdateResources(dto.getResources(), existing);
        saveOrUpdateEquipments(dto.getVehicleInstance().getEquipments(), existing.getVehicleInstance());
    }


    /**
     * Funciones de soporte
     */

    private void saveOrUpdateEquipments(List<CarInstanceEquipmentDto> equipments, VehicleInstanceEntity vehicleInstance) {
        if (equipments == null || equipments.isEmpty() || vehicleInstance == null) return;
        carInstanceEquipmentEntityRepository.deleteByVehicleInstanceId(vehicleInstance.getId());
        List<CarInstanceEquipmentEntity> entities = mapper.toCarInstanceEquipmentEntityList(equipments);
        entities.forEach(entity -> {
            int id = ThreadLocalRandom.current().nextInt(1000, 10000);
            entity.setId(id);
            entity.setVehicleInstance(vehicleInstance);
            if(entity.getType() == null){
                entity.setType(EquipmentTypeEntity.builder()
                        .id("Other")
                        .build());
            }
        });
        carInstanceEquipmentEntityRepository.saveAll(entities);
    }

    private void saveOrUpdateResources(List<CarOfferResourceDto> resources, OfferEntity offer) {
        if (resources == null || resources.isEmpty()) return;
        carOfferResourceRepository.deleteByOfferId(offer.getId());
        List<CarOfferResourceEntity> resourceEntities = resources.stream()
                .map(dto -> buildCarOfferResourceEntity(dto, offer))
                .toList();
        carOfferResourceRepository.saveAll(resourceEntities);
    }

    private CarOfferResourceEntity buildCarOfferResourceEntity(CarOfferResourceDto dto, OfferEntity offer) {
        ResourceTypeEntity type = ResourceTypeEntity.builder()
                .id("UrlImage")
                .build();
        byte[] image = convertUrlToBytes(dto.getResource());
        int id = ThreadLocalRandom.current().nextInt(1000, 10000);
        CarOfferResourceEntity resourceEntity = CarOfferResourceEntity.builder()
                .id(id)
                .offer(offer)
                .resourceType(type)
                .resource(image)
                .build();
        return resourceEntity;
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
                .id(offer.getJsonCarOfferId() != null ? offer.getJsonCarOfferId() : UUID.randomUUID())
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
                    Long id = ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
                    model.setId(id);
                    model.setEnabled(true); // Asegúrate de habilitarlo
                    return vehicleModelRepository.save(model);
                });
    }
}
