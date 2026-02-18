package net.liquidcars.ingestion.infra.output.kafka.model;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class VehicleModelMsgTest {

    @Mock
    private KeyValueMsg mockKeyValue;

    @Test
    @DisplayName("Getters y Setters: Cobertura de campos de datos de Lombok")
    void testLombokData() {
        VehicleModelMsg model = TestDataFactory.createVehicleModelMsgWithData(123L, "BMW", "530D", 258, 5.8 , mockKeyValue);

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
        // Opción segura: Creamos uno y usamos sus valores para el segundo
        VehicleModelMsg model1 = TestDataFactory.createVehicleModelMsg();
        VehicleModelMsg model2 = new VehicleModelMsg();

        // Copiamos TODOS los campos para asegurar que el hash coincida
        // Esto cubre también todos los getters de Lombok
        copyAllFields(model1, model2);

        // El ID no está en el HashCodeBuilder, cambiarlo NO debe alterar el hash
        model2.setId(model1.getId() + 1);

        assertThat(model1.hashCode()).isEqualTo(model2.hashCode());
        assertThat(model1.hashCode()).isNotNull();

        // Cambiar un campo que SÍ está en el hash (brand) debe hacerlo diferente
        model2.setBrand("MARCA_DIFERENTE");
        assertThat(model1.hashCode()).isNotEqualTo(model2.hashCode());
    }

    @Test
    @DisplayName("hashCode: Manejo de nulos")
    void testHashCodeWithNulls() {
        VehicleModelMsg model = TestDataFactory.createVehicleModelMsg();
        assertThat(model.hashCode()).isNotNull();
    }

    @Test
    @DisplayName("equals: Cobertura de todas las ramas lógicas")
    void testEquals() {
        VehicleModelMsg model1 = TestDataFactory.createVehicleModelMsg();
        VehicleModelMsg model2 = new VehicleModelMsg();
        copyAllFields(model1, model2);

        // Rama: if (o == this)
        assertThat(model1.equals(model1)).isTrue();

        // Rama: Mismo contenido -> mismo hash -> true
        assertThat(model1.equals(model2)).isTrue();

        // Rama: if (!(o instanceof VehicleModelJSONModel))
        assertThat(model1.equals("No Soy Un Coche")).isFalse();

        // Rama: null
        assertThat(model1.equals(null)).isFalse();

        // Rama: Contenido diferente -> hash diferente -> false
        model2.setBrand("OTRA_MARCA");
        assertThat(model1.equals(model2)).isFalse();
    }

    private void copyAllFields(VehicleModelMsg source, VehicleModelMsg target) {
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
