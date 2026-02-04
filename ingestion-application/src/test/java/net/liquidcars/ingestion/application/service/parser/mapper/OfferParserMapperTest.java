package net.liquidcars.ingestion.application.service.parser.mapper;

import net.liquidcars.ingestion.application.service.parser.model.OfferJSONModel;
import net.liquidcars.ingestion.application.service.parser.model.OfferXMLModel;
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

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getBrand()).isEqualTo(jsonModel.getBrand());
    }

    @Test
    void shouldMapOfferXMLToDto() {
        OfferXMLModel xmlModel = OfferXMLFactory.getOfferXMLModel();

        OfferDto result = mapper.toOfferDto(xmlModel);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getModel()).isEqualTo(xmlModel.getModel());
        assertThat(result.getVehicleType().name()).isEqualTo(xmlModel.getVehicleType().name());
    }

    @Test
    void shouldReturnNullWhenInputsAreNull() {
        assertThat(mapper.toOfferDto((OfferJSONModel) null)).isNull();
        assertThat(mapper.toOfferDto((OfferXMLModel) null)).isNull();
    }

    @Test
    void shouldHandleNullEnumsInsideModels() {
        OfferJSONModel jsonModel = new OfferJSONModel();
        jsonModel.setVehicleType(null);
        jsonModel.setStatus(null);

        OfferDto result = mapper.toOfferDto(jsonModel);

        assertThat(result.getVehicleType()).isNull();
        assertThat(result.getStatus()).isNull();
    }

    @Test
    void shouldMapAllVehicleTypeJSONValues() {
        for (OfferJSONModel.VehicleTypeJSON type : OfferJSONModel.VehicleTypeJSON.values()) {
            OfferJSONModel model = new OfferJSONModel();
            model.setVehicleType(type);

            OfferDto result = mapper.toOfferDto(model);

            assertThat(result.getVehicleType()).isNotNull();
            assertThat(result.getVehicleType().name()).isEqualTo(type.name());
        }
    }

    @Test
    void shouldMapAllOfferStatusXMLValues() {
        for (OfferXMLModel.OfferStatusXML status : OfferXMLModel.OfferStatusXML.values()) {
            OfferXMLModel model = new OfferXMLModel();
            model.setStatus(status);

            OfferDto result = mapper.toOfferDto(model);

            assertThat(result.getStatus()).isNotNull();
            assertThat(result.getStatus().name()).isEqualTo(status.name());
        }
    }


    @Test
    void shouldMapVehicleTypeJSONToDto() {
        for (OfferJSONModel.VehicleTypeJSON type : OfferJSONModel.VehicleTypeJSON.values()) {
            OfferDto.VehicleTypeDto result = mapper.vehicleTypeJSONToVehicleTypeDto(type);
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(type.name());
        }

        assertThat(mapper.vehicleTypeJSONToVehicleTypeDto(null)).isNull();
    }

    @Test
    void shouldMapVehicleTypeXMLToDto() {
        for (OfferXMLModel.VehicleTypeXML type : OfferXMLModel.VehicleTypeXML.values()) {
            OfferDto.VehicleTypeDto result = mapper.vehicleTypeXMLToVehicleTypeDto(type);
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(type.name());
        }

        assertThat(mapper.vehicleTypeXMLToVehicleTypeDto(null)).isNull();
    }

    @Test
    void shouldMapOfferStatusJSONToDto() {
        for (OfferJSONModel.OfferStatusJSON status : OfferJSONModel.OfferStatusJSON.values()) {
            OfferDto.OfferStatusDto result = mapper.offerStatusJSONToOfferStatusDto(status);
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(status.name());
        }

        assertThat(mapper.offerStatusJSONToOfferStatusDto(null)).isNull();
    }

    @Test
    void shouldMapOfferStatusXMLToDto() {
        for (OfferXMLModel.OfferStatusXML status : OfferXMLModel.OfferStatusXML.values()) {
            OfferDto.OfferStatusDto result = mapper.offerStatusXMLToOfferStatusDto(status);
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(status.name());
        }

        assertThat(mapper.offerStatusXMLToOfferStatusDto(null)).isNull();
    }

    @Test
    void shouldHandleNullInsideXMLModel() {
        OfferXMLModel xmlModel = new OfferXMLModel();
        xmlModel.setVehicleType(null);
        xmlModel.setStatus(null);

        OfferDto result = mapper.toOfferDto(xmlModel);

        assertThat(result).isNotNull();
        assertThat(result.getVehicleType()).isNull();
        assertThat(result.getStatus()).isNull();
    }
}