package net.liquidcars.ingestion.infra.output.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferInfraKafkaProducerMapperTest {

    private final OfferInfraKafkaProducerMapper mapper = Mappers.getMapper(OfferInfraKafkaProducerMapper.class);

    @Test
    @DisplayName("Should map OfferDto to OfferMsg correctly")
    void shouldMapDtoToMsg() {
        OfferDto sourceDto = OfferDtoFactory.getOfferDto();

        OfferMsg result = mapper.toOfferMsg(sourceDto);

        assertThat(result).isNotNull();

        assertThat(result.getId()).isEqualTo(sourceDto.getId());
        assertThat(result.getExternalId()).isEqualTo(sourceDto.getExternalId());
        assertThat(result.getBrand()).isEqualTo(sourceDto.getBrand());
        assertThat(result.getModel()).isEqualTo(sourceDto.getModel());
        assertThat(result.getYear()).isEqualTo(sourceDto.getYear());
        assertThat(result.getPrice()).isEqualByComparingTo(sourceDto.getPrice());

        assertThat(result.getVehicleType().name()).isEqualTo(sourceDto.getVehicleType().name());
        assertThat(result.getStatus().name()).isEqualTo(sourceDto.getStatus().name());

        assertThat(result.getCreatedAt()).isEqualTo(sourceDto.getCreatedAt());
        assertThat(result.getUpdatedAt()).isEqualTo(sourceDto.getUpdatedAt());
    }

    @Test
    @DisplayName("Should return null when source DTO is null")
    void shouldReturnNullWhenSourceIsNull() {
        OfferMsg result = mapper.toOfferMsg(null);

        assertThat(result).isNull();
    }

    @ParameterizedTest
    @EnumSource(OfferDto.VehicleTypeDto.class)
    @DisplayName("Should map all VehicleType enum constants")
    void shouldMapVehicleTypeEnum(OfferDto.VehicleTypeDto source) {
        OfferDto dto = new OfferDto();
        dto.setVehicleType(source);

        OfferMsg result = mapper.toOfferMsg(dto);

        assertThat(result.getVehicleType().name()).isEqualTo(source.name());
    }

    @ParameterizedTest
    @EnumSource(OfferDto.OfferStatusDto.class)
    @DisplayName("Should map all OfferStatus enum constants")
    void shouldMapOfferStatusEnum(OfferDto.OfferStatusDto source) {
        OfferDto dto = new OfferDto();
        dto.setStatus(source);

        OfferMsg result = mapper.toOfferMsg(dto);

        assertThat(result.getStatus().name()).isEqualTo(source.name());
    }
}
