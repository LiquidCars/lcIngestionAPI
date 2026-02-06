package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.VehicleModelDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.repository.VehicleModelSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {
    private final VehicleModelSQLRepository vehicleModelRepository;
    private final OfferSQLRepository offerSqlRepository;
    private final OfferInfraSQLMapper mapper;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processOffer(OfferDto offer) {
        log.info("Processing SQL persistence for id: {}", offer.getId());
        /* TODO Hay objetos de offerdto que no existen en la tabla de bd */
        try {
            // Builds the JsonOfferEntity TODO
            JsonOfferEntity jsonOfferEntity = buildJsonEntity(offer);
            OfferEntity entity = mapper.toEntity(offer);
            entity.setJsonCarOffer(jsonOfferEntity);
            // If the model doesn't exist on bd, we save it before the offer TODO
            ensureVehicleModelExists(offer.getVehicleInstance().getVehicleModel());
            offerSqlRepository.findById(offer.getId())
                    .ifPresentOrElse(
                            existing -> updateIfNewer(existing, entity),
                            () -> offerSqlRepository.save(entity)
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

    private JsonOfferEntity buildJsonEntity(OfferDto offer) {
        JsonObjectTypeEntity type = JsonObjectTypeEntity.builder()
                .id("caroffer")
                .build();
        JsonObjectEntity baseObject = JsonObjectEntity.builder()
                .id(offer.getJsonCarOfferId())
                .jsonObjectType(type)
                .createdAt(OffsetDateTime.now())
                .build();
        java.util.Map<String, Object> jsonMap = objectMapper.convertValue(
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

    private void ensureVehicleModelExists(VehicleModelDto dto) {
        vehicleModelRepository.findById(dto.getId())
                .orElseGet(() -> {
                    // Crear el modelo con TODOS sus datos
                    VehicleModelEntity model = VehicleModelEntity.builder()
                            .id(dto.getId())
                            .brand(dto.getBrand())
                            .model(dto.getModel())
                            .version(dto.getVersion())
                            .bodyType(BodyTypesEntity.builder().id(dto.getBodyType().getKey().toString()).build())
                            .numDoors(dto.getNumDoors())
                            .cv(dto.getCv())
                            .numCylinders(dto.getNumCylinders())
                            .displacement(dto.getDisplacement())
                            .urbanConsumption(dto.getUrbanConsumption())
                            .roadConsumption(dto.getRoadConsumption())
                            .avgConsumption(dto.getAvgConsumption())
                            .numGears(dto.getNumGears())
                            .kgWeight(dto.getKgWeight())
                            .changeType(ChangeTypesEntity.builder().id(dto.getChangeType().getKey().toString()).build())
                            .fuelType(FuelTypesEntity.builder().id(dto.getFuelType().getKey().toString()).build())
                            .numSeats(dto.getNumSeats())
                            .drivetrainType(DriveTrainTypeEntity.builder().id(dto.getDrivetrainType().getKey().toString()).build())
                            .euroTaxCode(dto.getEuroTaxCode())
                            .environmentalBadge(EnvironmentalBadgeEntity.builder().id(dto.getEnvironmentalBadge().getKey().toString()).build())
                            .cmWidth(dto.getCmWidth())
                            .cmLength(dto.getCmLength())
                            .cmHeight(dto.getCmHeight())
                            .litresTrunk(dto.getLitresTrunk())
                            .litresTank(dto.getLitresTank())
                            .maxSpeed(dto.getMaxSpeed())
                            .maxEmissions(dto.getMaxEmissions())
                            .acceleration(dto.getAcceleration())
                            .hash(0)
                            .enabled(true)
                            .build();

                    return vehicleModelRepository.save(model);
                });
    }

    private void updateIfNewer(OfferEntity existing, OfferEntity incoming) {
        if (incoming.getCreatedAt().isAfter(existing.getCreatedAt())) {
            log.debug("Updating existing offer. ID: {}", incoming.getId());
            incoming.setId(existing.getId());
            offerSqlRepository.save(incoming);
        } else {
            log.debug("Incoming offer is older than existing one. Skipping update. ID: {}", incoming.getId());
        }
    }
}
