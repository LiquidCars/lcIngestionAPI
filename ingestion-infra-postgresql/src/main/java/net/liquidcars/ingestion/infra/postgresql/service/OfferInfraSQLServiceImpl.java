package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.VehicleModelDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.IngestionReportRepository;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.repository.VehicleModelSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {
    private final VehicleModelSQLRepository vehicleModelRepository;
    private final OfferSQLRepository offerSqlRepository;
    private final OfferInfraSQLMapper mapper;
    private final ObjectMapper objectMapper;
    private final IngestionReportRepository reportRepository;

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
                            existingOffer -> updateIfNewer(existingOffer, entity),
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

    @Override
    @Transactional
    public void processIngestionReport(IngestionReportDto ingestionReportDto) {
        log.info("Processing SQL Report for Job: {}", ingestionReportDto.getJobId());

        try {
            // 1. Persist the report record
            reportRepository.save(mapper.toIngestionReportEntity(ingestionReportDto));
        } catch (Exception e) {
            log.error("Critical error processing SQL report for Job: {}", ingestionReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL report processing error")
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
            int offersDeleted = offerSqlRepository.deleteObsoleteOffers(threshold);
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

    @Override
    public List<IngestionReportDto> getPendingReports(){
        return mapper.toIngestionReportDtoList(reportRepository.findByProcessedFalse());
    }

    @Transactional
    @Override
    public void syncPendingReports(List<IngestionReportDto> pendingReports) {
        for (IngestionReportDto report : pendingReports) {
            try {
                if ("FAILED".equals(report.getStatus())) {
                    offerSqlRepository.deleteByJobIdentifier(report.getJobId());
                } else {
                    offerSqlRepository.updateBatchStatusByJobIdentifier(report.getJobId(), "COMPLETED");
                }
                IngestionReportEntity reportEntity = mapper.toIngestionReportEntity(report);
                reportEntity.setProcessed(true);
                reportRepository.save(reportEntity);
            } catch (Exception e) {
                log.error("Error syncing report {}", report.getJobId(), e);
            }
        }
    }

}
