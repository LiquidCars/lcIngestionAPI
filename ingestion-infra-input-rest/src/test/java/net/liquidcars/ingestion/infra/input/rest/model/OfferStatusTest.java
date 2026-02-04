package net.liquidcars.ingestion.infra.input.rest.model;

import net.liquidcars.ingestion.factory.OfferStatusFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OfferStatusTest {

    @RepeatedTest(5)
    @DisplayName("Should generate a valid random OfferStatus from the factory")
    void shouldGetRandomStatusFromFactory() {
        OfferStatus status = OfferStatusFactory.getOfferStatus();

        assertThat(status).isNotNull();
        assertThat(status).isIn((Object[]) OfferStatus.values());
    }

    @ParameterizedTest
    @EnumSource(OfferStatus.class)
    @DisplayName("fromValue should map each String to the corresponding Enum")
    void fromValueShouldReturnCorrectEnum(OfferStatus expectedStatus) {
        String valueToTest = expectedStatus.getValue();

        OfferStatus result = OfferStatus.fromValue(valueToTest);

        assertThat(result).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("Should throw an exception when using an invalid value from the factory")
    void fromValueShouldThrowExceptionOnInvalidValue() {
        String invalidValue = OfferStatusFactory.getInvalidOfferStatus();

        assertThatThrownBy(() -> OfferStatus.fromValue(invalidValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unexpected value '" + invalidValue + "'");
    }

    @Test
    @DisplayName("toString should return the exact associated string value")
    void toStringShouldMatchValue() {
        OfferStatus status = OfferStatusFactory.getOfferStatus();

        assertThat(status.toString()).isEqualTo(status.getValue());
    }
}
