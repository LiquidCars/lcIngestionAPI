package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CarOfferSellerTypeEnumTest {

    @ParameterizedTest
    @EnumSource(CarOfferSellerTypeEnum.class)
    @DisplayName("Debería validar getValue, toString y fromValue para cada constante")
    void testEnumMethods(CarOfferSellerTypeEnum type) {
        // Test getValue()
        String val = type.getValue();
        assertThat(val).isNotNull();

        // Test toString() - Debe coincidir con el valor interno
        assertThat(type.toString()).isEqualTo(val);

        // Test fromValue() con valores válidos (Cubre el bucle for y el return)
        assertThat(CarOfferSellerTypeEnum.fromValue(val)).isEqualTo(type);
    }

    @Test
    @DisplayName("Debería lanzar IllegalArgumentException ante un valor inválido")
    void testFromValueInvalid() {
        // Cubre la línea del throw new IllegalArgumentException
        String invalidValue = "not_a_valid_seller_type";
        assertThatThrownBy(() -> CarOfferSellerTypeEnum.fromValue(invalidValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected value '" + invalidValue + "'");
    }

    @Test
    @DisplayName("Cobertura de métodos estáticos integrados de Java")
    void testStandardEnumMethods() {
        // Cubre .values()
        CarOfferSellerTypeEnum[] values = CarOfferSellerTypeEnum.values();
        assertThat(values).containsExactlyInAnyOrder(
                CarOfferSellerTypeEnum.PROFESSIONALSELLER,
                CarOfferSellerTypeEnum.PRIVATESELLER
        );

        // Cubre .valueOf()
        assertThat(CarOfferSellerTypeEnum.valueOf("PROFESSIONALSELLER"))
                .isEqualTo(CarOfferSellerTypeEnum.PROFESSIONALSELLER);
    }
}
