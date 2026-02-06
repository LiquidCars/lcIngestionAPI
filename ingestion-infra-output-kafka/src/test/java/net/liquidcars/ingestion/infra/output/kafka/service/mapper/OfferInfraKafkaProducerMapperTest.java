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
        assertThat(result.getChannelReference()).isEqualTo(sourceDto.getChannelReference());
        assertThat(result.getVehicleInstance().getPlate()).isEqualTo(sourceDto.getVehicleInstance().getPlate());
        assertThat(result.getPickUpAddress().getAddress().getExtendedAddress()).isEqualTo(sourceDto.getPickUpAddress().getAddress().getExtendedAddress());
        assertThat(result.getMail()).isEqualTo(sourceDto.getMail());
        assertThat(result.getPrice().getAmount()).isEqualByComparingTo(sourceDto.getPrice().getAmount());
    }

    @Test
    @DisplayName("Should return null when source DTO is null")
    void shouldReturnNullWhenSourceIsNull() {
        OfferMsg result = mapper.toOfferMsg(null);

        assertThat(result).isNull();
    }

}
