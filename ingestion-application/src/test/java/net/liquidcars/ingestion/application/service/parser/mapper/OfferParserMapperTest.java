package net.liquidcars.ingestion.application.service.parser.mapper;

import net.liquidcars.ingestion.application.service.parser.model.JSON.OfferJSONModel;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferJSONFactory;
import net.liquidcars.ingestion.factory.OfferXMLFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfferParserMapperTest {

    private final OfferParserMapperImpl mapper = new OfferParserMapperImpl();

    @Test
    void shouldMapOfferJSONToDto() {
        OfferJSONModel jsonModel = OfferJSONFactory.getOfferJSONModel();

        OfferDto result = mapper.toOfferDto(jsonModel);

        assertThat(result.getId()).isEqualTo(jsonModel.getId());
        assertThat(result.getChannelReference()).isEqualTo(jsonModel.getChannelReference());
        assertThat(result.getVehicleInstance().getPlate()).isEqualTo(jsonModel.getVehicleInstance().getPlate());
        assertThat(result.getPickUpAddress().getAddress().getExtendedAddress()).isEqualTo(jsonModel.getPickUpAddress().getAddress().getExtendedAddress());
        assertThat(result.getMail()).isEqualTo(jsonModel.getMail());
        assertThat(result.getPrice().getAmount()).isEqualByComparingTo(jsonModel.getPrice().getAmount());
    }

    @Test
    void shouldMapOfferXMLToDto() {
        OfferXMLModel xmlModel = OfferXMLFactory.getOfferXMLModel();

        OfferDto result = mapper.toOfferDto(xmlModel);

        assertThat(result.getId()).isEqualTo(xmlModel.getId());
        assertThat(result.getChannelReference()).isEqualTo(xmlModel.getChannelReference());
        assertThat(result.getVehicleInstance().getPlate()).isEqualTo(xmlModel.getVehicleInstance().getPlate());
        assertThat(result.getPickUpAddress().getAddress().getExtendedAddress()).isEqualTo(xmlModel.getPickUpAddress().getAddress().getExtendedAddress());
        assertThat(result.getMail()).isEqualTo(xmlModel.getMail());
        assertThat(result.getPrice().getAmount()).isEqualByComparingTo(xmlModel.getPrice().getAmount());
    }

    @Test
    void shouldReturnNullWhenInputsAreNull() {
        assertThat(mapper.toOfferDto((OfferJSONModel) null)).isNull();
        assertThat(mapper.toOfferDto((OfferXMLModel) null)).isNull();
    }

}