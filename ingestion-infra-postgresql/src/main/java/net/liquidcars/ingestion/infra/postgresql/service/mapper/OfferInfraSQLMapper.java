package net.liquidcars.ingestion.infra.postgresql.service.mapper;

import net.liquidcars.ingestion.domain.model.*;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.time.OffsetDateTime.class })
public interface OfferInfraSQLMapper {

    @Mapping(target = "vehicleInstance", source = "vehicleInstance")
    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "priceNew", source = "priceNew.amount")
    @Mapping(target = "financedPrice", source = "financedPrice.amount")
    @Mapping(target = "currency", source = "price.currency", qualifiedByName = "currencyReference")
    @Mapping(target = "lastUpdated", source = "lastUpdated", qualifiedByName = "epochToOffset")
    @Mapping(target = "jsonCarOffer", ignore = true)
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    OfferEntity toEntity(OfferDto offer);

    @Mapping(target = "vehicleModel", source = "vehicleModel", qualifiedByName = "vehicleModelReference")
    @Mapping(target = "color", source = "color", qualifiedByName = "colorReference")
    @Mapping(target = "state", source = "state", qualifiedByName = "stateReference")
    VehicleInstanceEntity toVehicleInstanceEntity(VehicleInstanceDto dto);

    @Mapping(target = "bodyType", source = "bodyType", qualifiedByName = "bodyTypeReference")
    @Mapping(target = "changeType", source = "changeType", qualifiedByName = "changeTypeReference")
    @Mapping(target = "fuelType", source = "fuelType", qualifiedByName = "fuelTypeReference")
    @Mapping(target = "drivetrainType", source = "drivetrainType", qualifiedByName = "drivetrainTypeReference")
    @Mapping(target = "environmentalBadge", source = "environmentalBadge", qualifiedByName = "environmentalBadgeReference")
    VehicleModelEntity toVehicleModelEntity(VehicleModelDto dto);

    @Named("currencyReference")
    default CurrencyEntity currencyReference(String currencyCode) {
        if (currencyCode == null) return null;
        return CurrencyEntity.builder().id(currencyCode).build();
    }

    @Named("vehicleModelReference")
    default VehicleModelEntity vehicleModelReference(VehicleModelDto dto) {
        if (dto == null || dto.getId() == 0) return null;
        return VehicleModelEntity.builder().id(dto.getId()).build();
    }

    @Named("colorReference")
    default ColorEntity colorReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return ColorEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("stateReference")
    default CarInstanceTypesEntity stateReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return CarInstanceTypesEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("bodyTypeReference")
    default BodyTypesEntity bodyTypeReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return BodyTypesEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("changeTypeReference")
    default ChangeTypesEntity changeTypeReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return ChangeTypesEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("fuelTypeReference")
    default FuelTypesEntity fuelTypeReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return FuelTypesEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("drivetrainTypeReference")
    default DriveTrainTypeEntity drivetrainTypeReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return DriveTrainTypeEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("environmentalBadgeReference")
    default EnvironmentalBadgeEntity environmentalBadgeReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return EnvironmentalBadgeEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("epochToOffset")
    default OffsetDateTime mapEpoch(long epoch) {
        return OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(epoch),
                ZoneOffset.UTC
        );
    }

}
