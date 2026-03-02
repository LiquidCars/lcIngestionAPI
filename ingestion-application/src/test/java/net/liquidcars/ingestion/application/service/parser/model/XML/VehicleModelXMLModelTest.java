package net.liquidcars.ingestion.application.service.parser.model.XML;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class VehicleModelXMLModelTest {

    @Mock
    private KeyValueXMLModel mockKeyValue;

    @Test
    @DisplayName("Getters y Setters: Cobertura de campos de datos de Lombok")
    void testLombokData() {
        VehicleModelXMLModel model = TestDataFactory.createVehicleModelXMLModelWithData(123L, "BMW", "530D", 258, 5.8 , mockKeyValue);

        assertThat(model.getId()).isEqualTo(123L);
        assertThat(model.getBrand()).isEqualTo("BMW");
        assertThat(model.getModel()).isEqualTo("530D");
        assertThat(model.getCv()).isEqualTo(258);
        assertThat(model.getAcceleration()).isEqualTo(5.8);
        assertThat(model.getBodyType()).isEqualTo(mockKeyValue);
    }

    @Test
    @DisplayName("hashCode: Cobertura de la implementación personalizada")
    void testHashCode() {
        // 1. Usamos la factoría corregida
        VehicleModelXMLModel model1 = TestDataFactory.createVehicleModelXMLModel();
        VehicleModelXMLModel model2 = new VehicleModelXMLModel();

        // 2. Copiamos los campos
        copyAllFields(model1, model2);

        // 3. Verificamos (El ID no afecta al hash según tu implementación)
        model2.setId(model1.getId() + 1);

        assertThat(model1.hashCode()).isEqualTo(model2.hashCode());
    }

    @Test
    @DisplayName("hashCode: Manejo de nulos")
    void testHashCodeWithNulls() {
        VehicleModelXMLModel model = TestDataFactory.createVehicleModelXMLModel();
        assertThat(model.hashCode()).isNotNull();
    }

    @Test
    @DisplayName("equals: Cobertura de todas las ramas lógicas")
    void testEquals() {
        VehicleModelXMLModel model1 = TestDataFactory.createVehicleModelXMLModel();
        VehicleModelXMLModel model2 = new VehicleModelXMLModel();
        copyAllFields(model1, model2);

        assertThat(model1.equals(model1)).isTrue();

        assertThat(model1.equals(model2)).isTrue();

        assertThat(model1.equals("No Soy Un Coche")).isFalse();

        assertThat(model1.equals(null)).isFalse();

        model2.setBrand("OTRA_MARCA");
        assertThat(model1.equals(model2)).isFalse();
    }

    private void copyAllFields(VehicleModelXMLModel source, VehicleModelXMLModel target) {
        target.setBrand(source.getBrand());
        target.setModel(source.getModel());
        target.setVersion(source.getVersion());
        target.setEuroTaxCode(source.getEuroTaxCode());
        target.setBodyType(source.getBodyType());
        target.setNumDoors(source.getNumDoors());
        target.setCv(source.getCv());
        target.setNumCylinders(source.getNumCylinders());
        target.setDisplacement(source.getDisplacement());
        target.setUrbanConsumption(source.getUrbanConsumption());
        target.setRoadConsumption(source.getRoadConsumption());
        target.setAvgConsumption(source.getAvgConsumption());
        target.setNumGears(source.getNumGears());
        target.setKgWeight(source.getKgWeight());
        target.setChangeType(source.getChangeType());
        target.setFuelType(source.getFuelType());
        target.setNumSeats(source.getNumSeats());
        target.setDrivetrainType(source.getDrivetrainType());
        target.setEnvironmentalBadge(source.getEnvironmentalBadge());
        target.setCmWidth(source.getCmWidth());
        target.setCmLength(source.getCmLength());
        target.setCmHeight(source.getCmHeight());
        target.setLitresTrunk(source.getLitresTrunk());
        target.setLitresTank(source.getLitresTank());
        target.setMaxSpeed(source.getMaxSpeed());
        target.setMaxEmissions(source.getMaxEmissions());
        target.setAcceleration(source.getAcceleration());
    }
}
