package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IngestionPayloadTest {

    @Test
    @DisplayName("Cobertura total: Setters, Getters y Métodos Fluídos")
    void testAccessors() {
        IngestionPayload payload = new IngestionPayload();
        List<OfferRequest> offers = new ArrayList<>();
        List<String> idsToDelete = Arrays.asList("ID1", "ID2");

        // 1. Probar métodos fluídos (builder style)
        payload.offers(offers)
                .offersToDelete(idsToDelete);

        // 2. Probar setters tradicionales
        payload.setOffers(offers);
        payload.setOffersToDelete(idsToDelete);

        // 3. Verificar getters
        assertThat(payload.getOffers()).isEqualTo(offers);
        assertThat(payload.getOffersToDelete()).isEqualTo(idsToDelete);
    }

    @Test
    @DisplayName("Cobertura de lógica de listas (addItems con inicialización null)")
    void testListAdditionLogic() {
        IngestionPayload payload = new IngestionPayload();

        // Forzamos nulos para cubrir el 'if (this.xxx == null)' en los métodos add
        payload.setOffers(null);
        payload.setOffersToDelete(null);

        OfferRequest mockOffer = new OfferRequest();

        // Ejecutamos los métodos de añadir
        payload.addOffersItem(mockOffer);
        payload.addOffersToDeleteItem("DELETE_ME");

        assertThat(payload.getOffers()).hasSize(1).contains(mockOffer);
        assertThat(payload.getOffersToDelete()).hasSize(1).contains("DELETE_ME");
    }

    @Test
    @DisplayName("Cobertura de equals, hashCode y toString")
    void testLogicMethods() {
        IngestionPayload p1 = new IngestionPayload().addOffersToDeleteItem("1");
        IngestionPayload p2 = new IngestionPayload().addOffersToDeleteItem("1");
        IngestionPayload p3 = new IngestionPayload().addOffersToDeleteItem("2");

        // Equals: Identidad (this == o)
        assertThat(p1.equals(p1)).isTrue();

        // Equals: Valores iguales
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());

        // Equals: Casos false
        assertThat(p1).isNotEqualTo(p3);
        assertThat(p1).isNotEqualTo(null);
        assertThat(p1).isNotEqualTo(new Object());

        // toString e indentación (con valores y con nulls)
        assertThat(p1.toString()).contains("offersToDelete: [1]");

        IngestionPayload empty = new IngestionPayload();
        empty.setOffers(null); // Para cubrir toIndentedString(null)
        assertThat(empty.toString()).contains("offers: null");
    }
}
