package net.liquidcars.ingestion.application.service.parser.model.JSON;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VehicleInstanceJSONModelTest {

    @Test
    @DisplayName("Deben ser iguales si tienen los mismos datos, aunque el ID sea distinto")
    void shouldBeEqualWhenDataIsSameRegardlessOfId() {
        VehicleInstanceJSONModel vehicle1 = TestDataFactory.createVehicleInstanceJSON(1L, "1234ABC", "CHASSIS-1");

        VehicleInstanceJSONModel vehicle2 = TestDataFactory.createVehicleInstanceJSONModelWithSameData(vehicle1, 1L);

        assertThat(vehicle1).isEqualTo(vehicle2);
        assertThat(vehicle1.hashCode()).isEqualTo(vehicle2.hashCode());
    }

    @Test
    @DisplayName("No deben ser iguales si cambia un campo clave (matrícula)")
    void shouldNotBeEqualWhenPlateChanges() {
        VehicleInstanceJSONModel vehicle1 = TestDataFactory.createVehicleInstanceJSON(1L, "1234ABC", "CHASSIS-1");
        VehicleInstanceJSONModel vehicle2 = TestDataFactory.createVehicleInstanceJSON(1L, "9999XYZ", "CHASSIS-1");

        assertThat(vehicle1).isNotEqualTo(vehicle2);
        assertThat(vehicle1.hashCode()).isNotEqualTo(vehicle2.hashCode());
    }

    @Test
    @DisplayName("El hashCode debe ser siempre positivo por el Math.abs")
    void hashCodeShouldAlwaysBePositive() {
        VehicleInstanceJSONModel vehicle = TestDataFactory.createVehicleInstanceJSON(1L, "1234ABC", "CH-1");

        assertThat(vehicle.hashCode()).isPositive();
    }

    @Test
    @DisplayName("Verificar que Lombok funciona (Getter/Setter)")
    void testLombokAccessors() {
        VehicleInstanceJSONModel vehicle = new VehicleInstanceJSONModel();
        vehicle.setMileage(50000);

        assertThat(vehicle.getMileage()).isEqualTo(50000);
    }

    @Test
    @DisplayName("Debe calcular el hashCode incluso si el modelo de vehículo es nulo")
    void shouldHandleNullVehicleModelInHashCode() {
        VehicleInstanceJSONModel vehicle = TestDataFactory.createVehicleInstanceJSON(1L, "1234ABC", "CH-1");
        vehicle.setVehicleModel(null);

        // No debe lanzar NullPointerException y debe devolver un hash válido
        assertThat(vehicle.hashCode()).isPositive();
    }

    @Test
    @DisplayName("Equals debe retornar false al comparar con null o con clases distintas")
    void equalsShouldHandleNullAndDifferentClasses() {
        VehicleInstanceJSONModel vehicle = TestDataFactory.createVehicleInstanceJSON(1L, "1234ABC", "CH-1");

        assertThat(vehicle.equals(null)).isFalse();
        assertThat(vehicle.equals("Una cadena de texto")).isFalse();
    }

    @Test
    @DisplayName("Si A es igual a B, entonces B es igual a A")
    void equalsShouldBeSymmetric() {
        VehicleInstanceJSONModel vehicle1 = TestDataFactory.createVehicleInstanceJSON(1L, "1234ABC", "CH-1");
        VehicleInstanceJSONModel vehicle2 = TestDataFactory.createVehicleInstanceJSONModelWithSameData(vehicle1, 2L);

        assertThat(vehicle1).isEqualTo(vehicle2);
        assertThat(vehicle2).isEqualTo(vehicle1);
    }

    @Test
    @DisplayName("Cobertura total del contrato Equals")
    void testEqualsFullCoverage() {
        VehicleInstanceJSONModel vehicle = TestDataFactory.createVehicleInstanceJSON(1L, "A", "B");

        assertThat(vehicle).isEqualTo(vehicle);

        assertThat(vehicle).isNotEqualTo(null);

        assertThat(vehicle).isNotEqualTo(new Object());

        VehicleInstanceJSONModel identical = TestDataFactory.createVehicleInstanceJSONModelWithSameData(vehicle, 99L);
        assertThat(vehicle).isEqualTo(identical);

        identical.setPlate("CAMBIO");
        assertThat(vehicle).isNotEqualTo(identical);
    }
}
