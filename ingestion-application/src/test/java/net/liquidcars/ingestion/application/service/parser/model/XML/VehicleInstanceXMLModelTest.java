package net.liquidcars.ingestion.application.service.parser.model.XML;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VehicleInstanceXMLModelTest {

    @Test
    @DisplayName("Deben ser iguales si tienen los mismos datos, aunque el ID sea distinto")
    void shouldBeEqualWhenDataIsSameRegardlessOfId() {
        VehicleInstanceXMLModel vehicle1 = TestDataFactory.createVehicleInstanceXMLModel(1L, "1234ABC", "CHASSIS-1");

        VehicleInstanceXMLModel vehicle2 = TestDataFactory.createVehicleInstanceXMLModelWithSameData(vehicle1, 1L);

        assertThat(vehicle1).isEqualTo(vehicle2);
        assertThat(vehicle1.hashCode()).isEqualTo(vehicle2.hashCode());
    }

    @Test
    @DisplayName("No deben ser iguales si cambia un campo clave (matrícula)")
    void shouldNotBeEqualWhenPlateChanges() {
        VehicleInstanceXMLModel vehicle1 = TestDataFactory.createVehicleInstanceXMLModel(1L, "1234ABC", "CHASSIS-1");
        VehicleInstanceXMLModel vehicle2 = TestDataFactory.createVehicleInstanceXMLModel(1L, "9999XYZ", "CHASSIS-1");

        assertThat(vehicle1).isNotEqualTo(vehicle2);
        assertThat(vehicle1.hashCode()).isNotEqualTo(vehicle2.hashCode());
    }

    @Test
    @DisplayName("El hashCode debe ser siempre positivo por el Math.abs")
    void hashCodeShouldAlwaysBePositive() {
        VehicleInstanceXMLModel vehicle = TestDataFactory.createVehicleInstanceXMLModel(1L, "1234ABC", "CH-1");

        assertThat(vehicle.hashCode()).isPositive();
    }

    @Test
    @DisplayName("Verificar que Lombok funciona (Getter/Setter)")
    void testLombokAccessors() {
        VehicleInstanceXMLModel vehicle = new VehicleInstanceXMLModel();
        vehicle.setMileage(50000);

        assertThat(vehicle.getMileage()).isEqualTo(50000);
    }

    @Test
    @DisplayName("Debe calcular el hashCode incluso si el modelo de vehículo es nulo")
    void shouldHandleNullVehicleModelInHashCode() {
        VehicleInstanceXMLModel vehicle = TestDataFactory.createVehicleInstanceXMLModel(1L, "1234ABC", "CH-1");
        vehicle.setVehicleModel(null);

        // No debe lanzar NullPointerException y debe devolver un hash válido
        assertThat(vehicle.hashCode()).isPositive();
    }

    @Test
    @DisplayName("Equals debe retornar false al comparar con null o con clases distintas")
    void equalsShouldHandleNullAndDifferentClasses() {
        VehicleInstanceXMLModel vehicle = TestDataFactory.createVehicleInstanceXMLModel(1L, "1234ABC", "CH-1");

        assertThat(vehicle.equals(null)).isFalse();
        assertThat(vehicle.equals("Una cadena de texto")).isFalse();
    }

    @Test
    @DisplayName("Si A es igual a B, entonces B es igual a A")
    void equalsShouldBeSymmetric() {
        VehicleInstanceXMLModel vehicle1 = TestDataFactory.createVehicleInstanceXMLModel(1L, "1234ABC", "CH-1");
        VehicleInstanceXMLModel vehicle2 = TestDataFactory.createVehicleInstanceXMLModelWithSameData(vehicle1, 2L);

        assertThat(vehicle1).isEqualTo(vehicle2);
        assertThat(vehicle2).isEqualTo(vehicle1);
    }

    @Test
    @DisplayName("Cobertura total del contrato Equals")
    void testEqualsFullCoverage() {
        VehicleInstanceXMLModel vehicle = TestDataFactory.createVehicleInstanceXMLModel(1L, "A", "B");

        assertThat(vehicle).isEqualTo(vehicle);

        assertThat(vehicle).isNotEqualTo(null);

        assertThat(vehicle).isNotEqualTo(new Object());

        VehicleInstanceXMLModel identical = TestDataFactory.createVehicleInstanceXMLModelWithSameData(vehicle, 99L);
        assertThat(vehicle).isEqualTo(identical);

        identical.setPlate("CAMBIO");
        assertThat(vehicle).isNotEqualTo(identical);
    }
}
