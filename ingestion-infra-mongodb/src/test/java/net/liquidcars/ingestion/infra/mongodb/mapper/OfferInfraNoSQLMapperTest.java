package net.liquidcars.ingestion.infra.mongodb.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferNoSQLEntityFactory;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class OfferInfraNoSQLMapperTest {

    private final OfferInfraNoSQLMapper mapper = Mappers.getMapper(OfferInfraNoSQLMapper.class);

    @Test
    void toEntity_ShouldMapGeneratedDtoToEntity() {
        // Arrange
        OfferDto dto = OfferDtoFactory.getOfferDto();

        OfferNoSQLEntity entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId().toString());

    }

    @Test
    void toDto_ShouldMapGeneratedEntityToDto() {
        OfferNoSQLEntity entity = OfferNoSQLEntityFactory.getOfferNoSQLEntity();

        OfferDto dto = mapper.toDto(entity);

        assertThat(dto).isNotNull();
        assertThat(entity.getId()).isEqualTo(dto.getId().toString());

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

}
