package net.liquidcars.ingestion.application.service.parser.model.XML;

import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferXMLModelTest {

    @Test
    @DisplayName("Debe ser válida cuando todos los campos obligatorios están presentes y el precio es > 0")
    void shouldBeValidWhenAllCriteriaAreMet() {
        OfferXMLModel offer = TestDataFactory.createValidOfferXMLModel();
        assertThat(offer.isValid()).isTrue();
    }

    @Test
    @DisplayName("No debe ser válida si el tipo de vendedor es nulo")
    void shouldBeInvalidWhenSellerTypeIsNull() {
        OfferXMLModel offer = TestDataFactory.createValidOfferXMLModel();
        offer.setSellerType(null);
        assertThat(offer.isValid()).isFalse();
    }

    @Test
    @DisplayName("No debe ser válida si el precio o su monto es nulo o cero")
    void shouldBeInvalidWhenPriceIsIncorrect() {
        OfferXMLModel offer = TestDataFactory.createValidOfferXMLModel();

        offer.setPrice(null);
        assertThat(offer.isValid()).as("Price null").isFalse();

        offer.setPrice(new MoneyXMLModel(null, null));
        assertThat(offer.isValid()).as("Amount null").isFalse();

        offer.getPrice().setAmount(BigDecimal.ZERO);
        assertThat(offer.isValid()).as("Amount zero").isFalse();
    }

    @Test
    @DisplayName("No debe ser válida si no tiene recursos (imágenes/documentos)")
    void shouldBeInvalidWhenResourcesAreMissing() {
        OfferXMLModel offer = TestDataFactory.createValidOfferXMLModel();

        offer.setResources(null);
        assertThat(offer.isValid()).as("Resources null").isFalse();

        offer.setResources(new ArrayList<>());
        assertThat(offer.isValid()).as("Resources empty").isFalse();
    }

    @Test
    @DisplayName("Validación de contexto de vendedor: Vendedor Privado")
    void shouldValidatePrivateSellerContext() {
        OfferXMLModel offer = TestDataFactory.createValidOfferXMLModel();
        offer.setSellerType(CarOfferSellerTypeEnumXMLModel.usedCar_PrivateSeller);

        offer.setPrivateOwnerRegisteredUserId(null);
        assertThat(offer.isValid()).as("Private seller without UUID").isFalse();

        offer.setPrivateOwnerRegisteredUserId(UUID.randomUUID());
        assertThat(offer.isValid()).as("Private seller with UUID").isTrue();
    }


    @Test
    @DisplayName("Verificar funcionamiento del Builder")
    void testBuilder() {
        UUID id = UUID.randomUUID();
        OfferXMLModel offer = OfferXMLModel.builder()
                .jsonCarOfferId(id)
                .guarantee(true)
                .build();

        assertThat(offer.getJsonCarOfferId()).isEqualTo(id);
        assertThat(offer.isGuarantee()).isTrue();
    }
}
