package net.liquidcars.ingestion.infra.input.rest.model;

import net.liquidcars.ingestion.factory.VehicleTypeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class VehicleTypeTest {

    @Test
    @DisplayName("Debería generar un VehicleType aleatorio no nulo usando Instancio")
    void shouldGenerateRandomVehicleType() {
        VehicleType type = VehicleTypeFactory.getVehicleType();

        assertThat(type).isNotNull();
        assertThat(type).isIn((Object[]) VehicleType.values());
    }

    @ParameterizedTest
    @EnumSource(VehicleType.class)
    @DisplayName("Debería retornar el Enum correcto desde su valor String (fromValue)")
    void fromValueShouldReturnCorrectEnum(VehicleType expectedType) {
        String valueToTest = expectedType.getValue();

        VehicleType result = VehicleType.fromValue(valueToTest);

        assertThat(result).isEqualTo(expectedType);
    }

    @Test
    @DisplayName("Debería lanzar excepción cuando el valor String no es válido")
    void fromValueShouldThrowExceptionOnInvalidValue() {
        String invalidValue = VehicleTypeFactory.getInvalidVehicleType();

        assertThatThrownBy(() -> VehicleType.fromValue(invalidValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected value '" + invalidValue + "'");
    }

    @Test
    @DisplayName("toString debería devolver el mismo valor que getValue")
    void toStringShouldMatchGetValue() {
        VehicleType type = VehicleTypeFactory.getVehicleType();

        assertThat(type.toString()).isEqualTo(type.getValue());
    }
}
