package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


public class VehicleModelTest {

    @Test
    @DisplayName("Debería funcionar el patrón de diseño Fluent (Builder-style)")
    void testFluentApi() {
        VehicleModel model = new VehicleModel()
                .id(100)
                .brand("Audi")
                .model("A4")
                .cv(150)
                .acceleration(new BigDecimal("8.5"));

        assertThat(model.getId()).isEqualTo(100);
        assertThat(model.getBrand()).isEqualTo("Audi");
        assertThat(model.getCv()).isEqualTo(150);
        assertThat(model.getAcceleration()).isEqualTo(new BigDecimal("8.5"));
    }

    @Test
    @DisplayName("Debería validar la igualdad (equals) y el hashCode")
    void testEqualsAndHashCode() {
        VehicleModel model1 = new VehicleModel().id(1).brand("Seat");
        VehicleModel model2 = new VehicleModel().id(1).brand("Seat");
        VehicleModel model3 = new VehicleModel().id(2).brand("BMW");

        assertThat(model1).isEqualTo(model2);
        assertThat(model1.hashCode()).isEqualTo(model2.hashCode());

        assertThat(model1).isNotEqualTo(model3);
        assertThat(model1.hashCode()).isNotEqualTo(model3.hashCode());
    }

    @Test
    @DisplayName("Cobertura total de equals (incluyendo identidad y casos nulos)")
    void testEqualsReflexiveAndEdgeCases() {
        VehicleModel model = new VehicleModel().id(1).brand("Seat");

        assertThat(model).isEqualTo(model);

        assertThat(model).isNotEqualTo(null);

        assertThat(model).isNotEqualTo("Soy un String, no un coche");

        VehicleModel modelSame = new VehicleModel().id(1).brand("Seat");
        assertThat(model).isEqualTo(modelSame);
    }

    @Test
    @DisplayName("Debería generar un toString que contenga los datos clave")
    void testToString() {
        VehicleModel model = new VehicleModel().brand("Ferrari").maxSpeed(320);

        String result = model.toString();

        assertThat(result)
                .contains("class VehicleModel")
                .contains("brand: Ferrari")
                .contains("maxSpeed: 320");
    }

    @Test
    @DisplayName("Debería manejar valores nulos correctamente en equals")
    void testEqualsWithNulls() {
        VehicleModel model1 = new VehicleModel();
        VehicleModel model2 = new VehicleModel();

        assertThat(model1).isEqualTo(model2);
        assertThat(model1).isNotEqualTo(null);
        assertThat(model1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Test de Setters y Getters para Cobertura de Código")
    void testAllSettersAndGetters() {
        VehicleModel model = new VehicleModel();
        KeyValue kv = new KeyValue();
        BigDecimal decimal = new BigDecimal("10.0");

        model.id(1)
                .brand("Toyota")
                .model("Corolla")
                .version("Hybrid Sport")
                .bodyType(kv)
                .numDoors(5)
                .cv(140)
                .numCylinders(4)
                .displacement(1798)
                .urbanConsumption(decimal)
                .roadConsumption(decimal)
                .avgConsumption(decimal)
                .numGears(6)
                .kgWeight(1300)
                .changeType(kv)
                .fuelType(kv)
                .numSeats(5)
                .drivetrainType(kv)
                .euroTaxCode("A123")
                .environmentalBadge(kv)
                .cmWidth(179)
                .cmLength(437)
                .cmHeight(143)
                .litresTrunk(361)
                .litresTank(43)
                .maxSpeed(180)
                .maxEmissions(95)
                .acceleration(new BigDecimal("7.9"));

        model.setId(1);
        model.setBrand("Toyota");
        model.setModel("Corolla");
        model.setVersion("Hybrid");
        model.setBodyType(kv);
        model.setNumDoors(5);
        model.setCv(140);
        model.setNumCylinders(4);
        model.setDisplacement(1800);
        model.setUrbanConsumption(decimal);
        model.setRoadConsumption(decimal);
        model.setAvgConsumption(decimal);
        model.setNumGears(6);
        model.setKgWeight(1400);
        model.setChangeType(kv);
        model.setFuelType(kv);
        model.setNumSeats(5);
        model.setDrivetrainType(kv);
        model.setEuroTaxCode("XYZ");
        model.setEnvironmentalBadge(kv);
        model.setCmWidth(180);
        model.setCmLength(450);
        model.setCmHeight(145);
        model.setLitresTrunk(350);
        model.setLitresTank(45);
        model.setMaxSpeed(180);
        model.setMaxEmissions(90);
        model.setAcceleration(decimal);

        assertThat(model.getId()).isEqualTo(1);
        assertThat(model.getBrand()).isEqualTo("Toyota");
        assertThat(model.getModel()).isEqualTo("Corolla");
        assertThat(model.getVersion()).isEqualTo("Hybrid");
        assertThat(model.getBodyType()).isEqualTo(kv);
        assertThat(model.getNumDoors()).isEqualTo(5);
        assertThat(model.getCv()).isEqualTo(140);
        assertThat(model.getNumCylinders()).isEqualTo(4);
        assertThat(model.getDisplacement()).isEqualTo(1800);
        assertThat(model.getUrbanConsumption()).isEqualTo(decimal);
        assertThat(model.getRoadConsumption()).isEqualTo(decimal);
        assertThat(model.getAvgConsumption()).isEqualTo(decimal);
        assertThat(model.getNumGears()).isEqualTo(6);
        assertThat(model.getKgWeight()).isEqualTo(1400);
        assertThat(model.getChangeType()).isEqualTo(kv);
        assertThat(model.getFuelType()).isEqualTo(kv);
        assertThat(model.getNumSeats()).isEqualTo(5);
        assertThat(model.getDrivetrainType()).isEqualTo(kv);
        assertThat(model.getEuroTaxCode()).isEqualTo("XYZ");
        assertThat(model.getEnvironmentalBadge()).isEqualTo(kv);
        assertThat(model.getCmWidth()).isEqualTo(180);
        assertThat(model.getCmLength()).isEqualTo(450);
        assertThat(model.getCmHeight()).isEqualTo(145);
        assertThat(model.getLitresTrunk()).isEqualTo(350);
        assertThat(model.getLitresTank()).isEqualTo(45);
        assertThat(model.getMaxSpeed()).isEqualTo(180);
        assertThat(model.getMaxEmissions()).isEqualTo(90);
        assertThat(model.getAcceleration()).isEqualTo(decimal);
    }

    @Test
    @DisplayName("Test de Métodos Fluídos (id, brand, etc.)")
    void testFluentMethods() {
        // OpenAPI genera métodos como .brand("...") que llaman al setter.
        // También hay que tocarlos para cobertura total.
        VehicleModel model = new VehicleModel().brand("Audi").id(5);

        assertThat(model.getBrand()).isEqualTo("Audi");
        assertThat(model.getId()).isEqualTo(5);
    }

}
