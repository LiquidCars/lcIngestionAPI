package net.liquidcars.ingestion.infra.output.kafka.model;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VehicleInstanceMsgTest {

    @Test
    @DisplayName("Deben ser iguales si tienen los mismos datos, aunque el ID sea distinto")
    void shouldBeEqualWhenDataIsSameRegardlessOfId() {
        VehicleInstanceMsg vehicle1 = TestDataFactory.createVehicleInstanceMsg(1L, "1234ABC", "CHASSIS-1");

        VehicleInstanceMsg vehicle2 = TestDataFactory.createVehicleInstanceMsgWithSameData(vehicle1, 1L);

        assertThat(vehicle1).isEqualTo(vehicle2);
        assertThat(vehicle1.hashCode()).isEqualTo(vehicle2.hashCode());
    }

    @Test
    @DisplayName("No deben ser iguales si cambia un campo clave (matrícula)")
    void shouldNotBeEqualWhenPlateChanges() {
        VehicleInstanceMsg vehicle1 = TestDataFactory.createVehicleInstanceMsg(1L, "1234ABC", "CHASSIS-1");
        VehicleInstanceMsg vehicle2 = TestDataFactory.createVehicleInstanceMsg(1L, "9999XYZ", "CHASSIS-1");

        assertThat(vehicle1).isNotEqualTo(vehicle2);
        assertThat(vehicle1.hashCode()).isNotEqualTo(vehicle2.hashCode());
    }

    @Test
    @DisplayName("El hashCode debe ser siempre positivo por el Math.abs")
    void hashCodeShouldAlwaysBePositive() {
        VehicleInstanceMsg vehicle = TestDataFactory.createVehicleInstanceMsg(1L, "1234ABC", "CH-1");

        assertThat(vehicle.hashCode()).isPositive();
    }

    @Test
    @DisplayName("Verificar que Lombok funciona (Getter/Setter)")
    void testLombokAccessors() {
        VehicleInstanceMsg vehicle = new VehicleInstanceMsg();
        vehicle.setMileage(50000);

        assertThat(vehicle.getMileage()).isEqualTo(50000);
    }

    @Test
    @DisplayName("Debe calcular el hashCode incluso si el modelo de vehículo es nulo")
    void shouldHandleNullVehicleModelInHashCode() {
        VehicleInstanceMsg vehicle = TestDataFactory.createVehicleInstanceMsg(1L, "1234ABC", "CH-1");
        vehicle.setVehicleModel(null);

        // No debe lanzar NullPointerException y debe devolver un hash válido
        assertThat(vehicle.hashCode()).isPositive();
    }

    @Test
    @DisplayName("Equals debe retornar false al comparar con null o con clases distintas")
    void equalsShouldHandleNullAndDifferentClasses() {
        VehicleInstanceMsg vehicle = TestDataFactory.createVehicleInstanceMsg(1L, "1234ABC", "CH-1");

        assertThat(vehicle.equals(null)).isFalse();
        assertThat(vehicle.equals("Una cadena de texto")).isFalse();
    }

    @Test
    @DisplayName("Si A es igual a B, entonces B es igual a A")
    void equalsShouldBeSymmetric() {
        VehicleInstanceMsg vehicle1 = TestDataFactory.createVehicleInstanceMsg(1L, "1234ABC", "CH-1");
        VehicleInstanceMsg vehicle2 = TestDataFactory.createVehicleInstanceMsgWithSameData(vehicle1, 2L);

        assertThat(vehicle1).isEqualTo(vehicle2);
        assertThat(vehicle2).isEqualTo(vehicle1);
    }

    @Test
    @DisplayName("Cobertura total del contrato Equals")
    void testEqualsFullCoverage() {
        VehicleInstanceMsg vehicle = TestDataFactory.createVehicleInstanceMsg(1L, "A", "B");

        assertThat(vehicle).isEqualTo(vehicle);

        assertThat(vehicle).isNotEqualTo(null);

        assertThat(vehicle).isNotEqualTo(new Object());

        VehicleInstanceMsg identical = TestDataFactory.createVehicleInstanceMsgWithSameData(vehicle, 99L);
        assertThat(vehicle).isEqualTo(identical);

        identical.setPlate("CAMBIO");
        assertThat(vehicle).isNotEqualTo(identical);
    }
}
