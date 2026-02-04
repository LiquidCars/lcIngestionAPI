package net.liquidcars.ingestion.infra.mongodb.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferNoSQLEntityFactory;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferInfraNoSQLMapperTest {

    private final OfferInfraNoSQLMapper mapper = Mappers.getMapper(OfferInfraNoSQLMapper.class);

    @Test
    void toEntity_ShouldMapGeneratedDtoToEntity() {
        // Arrange
        OfferDto dto = OfferDtoFactory.getOfferDto();

        OfferNoSQLEntity entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getExternalId()).isEqualTo(dto.getExternalId());

        if (dto.getCreatedAt() != null) {
            assertThat(entity.getCreatedAt()).isEqualTo(dto.getCreatedAt().toInstant());
        }
    }

    @Test
    void toDto_ShouldMapGeneratedEntityToDto() {
        OfferNoSQLEntity entity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();

        OfferDto dto = mapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getExternalId()).isEqualTo(entity.getExternalId());

        if (entity.getCreatedAt() != null) {
            assertThat(dto.getCreatedAt()).isEqualTo(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
    }

    @Test
    void customMappingMethods_ShouldHandleNullsExplicitly() {
        assertThat(mapper.map((OffsetDateTime) null)).isNull();
        assertThat(mapper.map((Instant) null)).isNull();
    }

    @Test
    void toEntity_ShouldReturnNull_WhenInputIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void shouldMapEnums_WhenInputsAreProvided() {
        OfferDto dto = OfferDtoFactory.getOfferDto();

        OfferNoSQLEntity entity = mapper.toEntity(dto);
        OfferDto backToDto = mapper.toDto(entity);

        assertThat(entity.getVehicleType()).isNotNull();
        assertThat(entity.getStatus()).isNotNull();

        assertThat(backToDto.getVehicleType()).isNotNull();
        assertThat(backToDto.getStatus()).isNotNull();
    }

    @Test
    void shouldReturnNullEnums_WhenEnumsAreNull() {
        OfferDto dto = new OfferDto();

        dto.setStatus(null);
        dto.setVehicleType(null);

        OfferNoSQLEntity entity = mapper.toEntity(dto);

        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getVehicleType()).isNull();
    }

    @Test
    void shouldReturnNullEnums_WhenEntityEnumsAreNull() {
        OfferNoSQLEntity entity = new OfferNoSQLEntity();
        entity.setVehicleType(null);
        entity.setStatus(null);

        OfferDto dto = mapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getVehicleType()).isNull();
        assertThat(dto.getStatus()).isNull();
    }

    @Test
    void shouldMapAllVehicleTypeEnumValuesInBothDirections() {
        for (OfferDto.VehicleTypeDto typeDto : OfferDto.VehicleTypeDto.values()) {
            OfferDto dto = OfferDtoFactory.getOfferDto();
            dto.setVehicleType(typeDto);

            OfferNoSQLEntity entity = mapper.toEntity(dto);

            assertThat(entity.getVehicleType().name()).isEqualTo(typeDto.name());
            assertThat(entity.getExternalId()).isEqualTo(dto.getExternalId());

            OfferNoSQLEntity entitySource = OfferNoSQLEntityFactory.getOfferNoSQLEntity();
            entitySource.setVehicleType(OfferNoSQLEntity.VehicleType.valueOf(typeDto.name()));

            OfferDto dtoResult = mapper.toDto(entitySource);

            assertThat(dtoResult.getVehicleType().name()).isEqualTo(entitySource.getVehicleType().name());
        }
    }

    @Test
    void shouldMapAllOfferStatusEnumValuesInBothDirections() {
        for (OfferDto.OfferStatusDto statusDto : OfferDto.OfferStatusDto.values()) {
            OfferDto dto = OfferDtoFactory.getOfferDto();
            dto.setStatus(statusDto);

            OfferNoSQLEntity entity = mapper.toEntity(dto);

            assertThat(entity.getStatus().name()).isEqualTo(statusDto.name());

            OfferNoSQLEntity entitySource = OfferNoSQLEntityFactory.getOfferNoSQLEntity();
            entitySource.setStatus(OfferNoSQLEntity.OfferStatus.valueOf(statusDto.name()));

            OfferDto dtoResult = mapper.toDto(entitySource);

            assertThat(dtoResult.getStatus().name()).isEqualTo(entitySource.getStatus().name());
        }
    }
}
