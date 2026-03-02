package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AddressTypeTest {

    @ParameterizedTest
    @EnumSource(AddressType.class)
    @DisplayName("Debería validar que cada enum devuelve su valor correcto y toString")
    void testEnumValues(AddressType type) {
        // Probamos getValue()
        assertThat(type.getValue()).isNotNull();

        // Probamos toString()
        assertThat(type.toString()).isEqualTo(type.getValue());

        // Probamos fromValue con valores válidos (esto cubre el bucle for)
        assertThat(AddressType.fromValue(type.getValue())).isEqualTo(type);
    }

    @Test
    @DisplayName("Debería lanzar excepción ante un valor no soportado en fromValue")
    void fromValueInvalid() {
        // Esto cubre la última línea: throw new IllegalArgumentException
        assertThatThrownBy(() -> AddressType.fromValue("INVALID_TYPE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected value 'INVALID_TYPE'");
    }

    @Test
    @DisplayName("Prueba de cobertura para la estructura básica del enum")
    void testEnumStructure() {
        // Cubre AddressType.values() y AddressType.valueOf()
        AddressType[] values = AddressType.values();
        assertThat(values).contains(AddressType.P_HOME, AddressType.B_RETAIL);

        AddressType home = AddressType.valueOf("P_HOME");
        assertThat(home).isEqualTo(AddressType.P_HOME);
    }
}
