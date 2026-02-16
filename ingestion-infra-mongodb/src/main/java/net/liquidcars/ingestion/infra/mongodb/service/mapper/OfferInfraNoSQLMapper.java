package net.liquidcars.ingestion.infra.mongodb.service.mapper;

import net.liquidcars.ingestion.domain.model.KeyValueDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.KeyValueNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.VehicleOfferNoSQLEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferInfraNoSQLMapper {

    @Mapping(target = "ownerReference", source = "externalIdInfo.ownerReference")
    @Mapping(target = "dealerReference", source = "externalIdInfo.dealerReference")
    @Mapping(target = "channelReference", source = "externalIdInfo.channelReference")
    @Mapping(target = "lastUpdated", expression = "java(System.currentTimeMillis())")
    DraftOfferNoSQLEntity toEntity(OfferDto offerDto);

    VehicleOfferNoSQLEntity toVehicleOfferNoSQLEntity(DraftOfferNoSQLEntity draftOfferNoSQLEntity);

    @Mapping(target = "externalIdInfo.ownerReference", source = "ownerReference")
    @Mapping(target = "externalIdInfo.dealerReference", source = "dealerReference")
    @Mapping(target = "externalIdInfo.channelReference", source = "channelReference")
    OfferDto toDto(DraftOfferNoSQLEntity offerNoSQLEntity);

    // --- Métodos de ayuda existentes ---

    default Instant map(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }

    default OffsetDateTime map(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    default KeyValueNoSQLEntity map(KeyValueDto<?, ?> keyValueDto) {
        if (keyValueDto == null) return null;
        return KeyValueNoSQLEntity.builder()
                .key(keyValueDto.getKey() != null ? keyValueDto.getKey().toString() : null)
                .value(keyValueDto.getValue() != null ? keyValueDto.getValue().toString() : null)
                .build();
    }

    default KeyValueDto<String, String> map(KeyValueNoSQLEntity keyValueNoSQLEntity) {
        if (keyValueNoSQLEntity == null) return null;
        return new KeyValueDto<>(keyValueNoSQLEntity.getKey(), keyValueNoSQLEntity.getValue());
    }

    default String map(UUID uuid) {
        return uuid != null ? uuid.toString() : null;
    }

    default UUID mapToUuid(String value) {
        return value != null ? UUID.fromString(value) : null;
    }
}
