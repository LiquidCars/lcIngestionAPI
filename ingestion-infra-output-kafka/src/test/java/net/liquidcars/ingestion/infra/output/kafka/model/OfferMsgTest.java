package net.liquidcars.ingestion.infra.output.kafka.model;

import net.liquidcars.ingestion.factory.OfferMsgFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferMsgTest {

    @Test
    @DisplayName("Should calculate hash correctly when internal hash is 0")
    void hashCode_ShouldCalculateWhenHashIsZero() {
        // Arrange
        OfferMsg offer = OfferMsgFactory.getOfferMsg();
        offer.setHash(0);
        offer.setFinancedText("Condiciones especiales");

        // Act
        int calculatedHash = offer.hashCode();

        // Assert
        assertThat(calculatedHash).isNotZero();
        assertThat(calculatedHash).isEqualTo(offer.getHashCodeCalc());
    }

    @Test
    @DisplayName("Should return stored hash if it is already set (non-zero)")
    void hashCode_ShouldReturnStoredHash() {
        // Arrange
        int originalHash = 12345;
        OfferMsg offer = OfferMsgFactory.getOfferMsg();
        offer.setHash(originalHash);
        offer.setFinancedText("Texto que cambiaría el hash si se calculara");

        // Act & Assert
        // No debe llamar a getHashCodeCalc(), debe devolver el valor del campo
        assertThat(offer.hashCode()).isEqualTo(originalHash);
    }

    @Test
    @DisplayName("Equals should return true for different objects with same hash")
    void equals_ShouldReturnTrueForSameHash() {
        // Arrange
        UUID sameId = UUID.randomUUID();
        OfferMsg offer1 = OfferMsg.builder()
                .id(sameId)
                .financedText("Oferta A")
                .price(new MoneyMsg(new BigDecimal("10000"), "EUR"))
                .build();

        OfferMsg offer2 = OfferMsg.builder()
                .id(sameId)
                .financedText("Oferta A")
                .price(new MoneyMsg(new BigDecimal("10000"), "EUR"))
                .build();

        // Act & Assert
        assertThat(offer1).isEqualTo(offer2);
        assertThat(offer1.hashCode()).isEqualTo(offer2.hashCode());
    }

    @Test
    @DisplayName("Equals should return false when hash differs")
    void equals_ShouldReturnFalseWhenDataDiffers() {
        // Arrange
        OfferMsg offer1 = OfferMsg.builder().financedText("Texto 1").build();
        OfferMsg offer2 = OfferMsg.builder().financedText("Texto 2").build();

        // Act & Assert
        assertThat(offer1).isNotEqualTo(offer2);
    }

    @Test
    @DisplayName("getHashCodeCalc should be null-safe for externalIdInfo")
    void getHashCodeCalc_ShouldHandleNullExternalIdInfo() {
        // Arrange
        OfferMsg offer = OfferMsgFactory.getOfferMsg();
        offer.setExternalIdInfo(null);

        // Act & Assert
        // No debe lanzar NullPointerException
        assertThat(offer.getHashCodeCalc()).isPositive();
    }

    @Test
    @DisplayName("Hash should change if ExternalIdInfo internal fields change")
    void getHashCodeCalc_ShouldIncludeExternalIdInfoFields() {
        // Arrange
        ExternalIdInfoMsg info1 = ExternalIdInfoMsg.builder().ownerReference("OWN-1").build();
        ExternalIdInfoMsg info2 = ExternalIdInfoMsg.builder().ownerReference("OWN-2").build();

        OfferMsg offer1 = OfferMsg.builder().externalIdInfo(info1).build();
        OfferMsg offer2 = OfferMsg.builder().externalIdInfo(info2).build();

        // Act & Assert
        assertThat(offer1.getHashCodeCalc()).isNotEqualTo(offer2.getHashCodeCalc());
    }

    @Test
    @DisplayName("Equals should return true when comparing the same instance")
    void equals_ShouldReturnTrueForSameInstance() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        // Esto cubre la rama: if (o == this) return true;
        assertThat(offer.equals(offer)).isTrue();
    }

    @Test
    @DisplayName("Equals should return false when comparing with null")
    void equals_ShouldReturnFalseWithNull() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        // Esto cubre parte de la rama: if (!(o instanceof OfferMsg))
        assertThat(offer.equals(null)).isFalse();
    }

    @Test
    @DisplayName("Equals should return false when comparing with different class")
    void equals_ShouldReturnFalseWithDifferentClass() {
        OfferMsg offer = OfferMsgFactory.getOfferMsg();
        String notAnOffer = "I am a string";

        // Esto cubre la otra parte de: if (!(o instanceof OfferMsg))
        assertThat(offer.equals(notAnOffer)).isFalse();
    }

}
