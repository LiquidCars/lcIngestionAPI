package net.liquidcars.ingestion.infra.output.kafka.model;

import net.liquidcars.ingestion.factory.OfferMsgFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferMsgTest {

    @Test
    @DisplayName("Should create a valid OfferMsg using the factory")
    void factoryShouldProduceValidObject() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        assertThat(offer).isNotNull();
        assertThat(offer.getId()).isNotNull();
    }

    @Test
    @DisplayName("isAvailable should return true only when status is ACTIVE")
    void isAvailableLogic() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        offer.setStatus(OfferMsg.OfferStatusMsg.ACTIVE);
        assertThat(offer.isAvailable()).isTrue();

        offer.setStatus(OfferMsg.OfferStatusMsg.SOLD);
        assertThat(offer.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isValid should return true for a correctly populated factory object")
    void isValidSuccess() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        offer.setExternalId("EXT-123");
        offer.setBrand("Toyota");
        offer.setModel("Corolla");
        offer.setYear(2023);
        offer.setPrice(new BigDecimal("25000"));

        assertThat(offer.isValid()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("isValid should return false if brand or model is missing")
    void isValidFailureEmptyFields(String invalidValue) {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        offer.setBrand(invalidValue);
        assertThat(offer.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false if price is zero or negative")
    void isValidFailurePrice() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        offer.setPrice(BigDecimal.ZERO);
        assertThat(offer.isValid()).isFalse();

        offer.setPrice(new BigDecimal("-10.50"));
        assertThat(offer.isValid()).isFalse();
    }

    @Test
    @DisplayName("isValid should return false if year is out of range")
    void isValidFailureYear() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();
        int nextYear = LocalDateTime.now().getYear() + 2;

        offer.setYear(1899);
        assertThat(offer.isValid()).isFalse();

        offer.setYear(nextYear);
        assertThat(offer.isValid()).isFalse();
    }
}
