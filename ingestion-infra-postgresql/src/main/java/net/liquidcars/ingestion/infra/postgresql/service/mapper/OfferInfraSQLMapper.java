package net.liquidcars.ingestion.infra.postgresql.service.mapper;

import net.liquidcars.ingestion.domain.model.*;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import org.mapstruct.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.time.OffsetDateTime.class })
public interface OfferInfraSQLMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "equipment", source = "equipment", qualifiedByName = "equipmentReference")
    @Mapping(target = "category", source = "category", qualifiedByName = "equipmentCategoryReference")
    @Mapping(target = "type", source = "type", qualifiedByName = "equipmentTypeReference")
    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "currency", source = "price.currency", qualifiedByName = "currencyReference")
    @Mapping(target = "vehicleInstance", ignore = true)
    CarInstanceEquipmentEntity toCarInstanceEquipmentEntity(CarInstanceEquipmentDto carInstanceEquipmentDto);

    List<CarInstanceEquipmentEntity> toCarInstanceEquipmentEntityList(List<CarInstanceEquipmentDto> dtoList);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addressType", source = "type", qualifiedByName = "addressTypeReference")
    @Mapping(target = "padName", source = "address.name")
    @Mapping(target = "longitude", source = "address.gpsLocation.longitude")
    @Mapping(target = "latitude", source = "address.gpsLocation.latitude")
    @Mapping(target = "streetNumber", source = "address.streetNumber")
    @Mapping(target = "streetAddress", source = "address.streetAddress")
    @Mapping(target = "extendedAddress", source = "address.extendedAddress")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "region", source = "address.region")
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "countryCode", source = "address.countryCode")
    @Mapping(target = "poBox", source = "address.poBox")
    void updateAddressFromDto(ParticipantAddressDto dto, @MappingTarget ParticipantAddressEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "addressType", source = "type", qualifiedByName = "addressTypeReference")
    @Mapping(target = "padName", source = "address.name")
    @Mapping(target = "longitude", source = "address.gpsLocation.longitude")
    @Mapping(target = "latitude", source = "address.gpsLocation.latitude")
    @Mapping(target = "streetNumber", source = "address.streetNumber")
    @Mapping(target = "streetAddress", source = "address.streetAddress")
    @Mapping(target = "extendedAddress", source = "address.extendedAddress")
    @Mapping(target = "postalCode", source = "address.postalCode")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "region", source = "address.region")
    @Mapping(target = "country", source = "address.country")
    @Mapping(target = "countryCode", source = "address.countryCode")
    @Mapping(target = "poBox", source = "address.poBox")
    ParticipantAddressEntity toParticipantAddressEntity(ParticipantAddressDto participantAddressDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jsonCarOffer", ignore = true)
    @Mapping(target = "vehicleInstance", source = "vehicleInstance")
    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "priceNew", source = "priceNew.amount")
    @Mapping(target = "financedPrice", source = "financedPrice.amount")
    @Mapping(target = "currency", source = "price.currency", qualifiedByName = "currencyReference")
    @Mapping(target = "lastUpdated", source = "lastUpdated", qualifiedByName = "epochToOffset")
    void updateEntityFromDto(OfferDto dto, @MappingTarget OfferEntity entity);

    @Mapping(target = "vehicleInstance", source = "vehicleInstance")
    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "priceNew", source = "priceNew.amount")
    @Mapping(target = "financedPrice", source = "financedPrice.amount")
    @Mapping(target = "currency", source = "price.currency", qualifiedByName = "currencyReference")
    @Mapping(target = "lastUpdated", source = "lastUpdated", qualifiedByName = "epochToOffset")
    @Mapping(target = "jsonCarOffer", ignore = true)
    @Mapping(target = "createdAt", expression = "java(OffsetDateTime.now())")
    @Mapping(target = "inventoryIds", source = "inventoryId", qualifiedByName = "uuidToSet")
    @Mapping(target = "ownerReference", source = "externalIdInfo.ownerReference")
    @Mapping(target = "dealerReference", source = "externalIdInfo.dealerReference")
    @Mapping(target = "channelReference", source = "externalIdInfo.channelReference")
    @Mapping(target = "hash", expression = "java(offer.hashCode())")
    OfferEntity toEntity(OfferDto offer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicleModel", ignore = true) // Se gestiona manualmente en el Service
    @Mapping(target = "color", source = "color", qualifiedByName = "colorReference")
    @Mapping(target = "state", source = "state", qualifiedByName = "stateReference")
    void updateVehicleInstanceFromDto(VehicleInstanceDto dto, @MappingTarget VehicleInstanceEntity entity);

    @Named("uuidToSet")
    default Set<UUID> uuidToSet(UUID inventoryId) {
        if (inventoryId == null) return null;
        return Collections.singleton(inventoryId);
    }

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
        if (epoch <= 0) return OffsetDateTime.now(ZoneOffset.UTC);

        return OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epoch),
                ZoneOffset.UTC
        );
    }

    @Named("addressTypeReference")
    default AddressTypeEntity addressTypeReference(AddressTypeDto typeDto) {
        if (typeDto == null) return null;
        return AddressTypeEntity.builder().id(typeDto.name()).build();
    }

    @Named("equipmentReference")
    default EquipmentsEntity equipmentReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return EquipmentsEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("equipmentCategoryReference")
    default EquipmentCategoryEntity equipmentCategoryReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return EquipmentCategoryEntity.builder().id(dto.getKey().toString()).build();
    }

    @Named("equipmentTypeReference")
    default EquipmentTypeEntity equipmentTypeReference(KeyValueDto dto) {
        if (dto == null || dto.getKey() == null) return null;
        return EquipmentTypeEntity.builder().id(dto.getKey().toString()).build();
    }

}
