package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CarOfferResourceTest {

    @Test
    @DisplayName("Cobertura total: Setters, Getters y Métodos Fluídos")
    void testAccessors() {
        CarOfferResource model = new CarOfferResource();
        KeyValue type = new KeyValue().key("IMG").value("Imagen");
        byte[] data = "test-data".getBytes();

        // 1. Probar métodos fluídos (Builder style)
        model.id(1)
                .type(type)
                .resource("http://resource.url")
                .compressedResource(data);

        // 2. Probar setters tradicionales (para Jacoco)
        model.setId(2);
        model.setType(type);
        model.setResource("http://new.url");
        model.setCompressedResource(data);

        // 3. Verificar con Getters
        assertThat(model.getId()).isEqualTo(2);
        assertThat(model.getType()).isEqualTo(type);
        assertThat(model.getResource()).isEqualTo("http://new.url");
        assertThat(model.getCompressedResource()).containsExactly(data);
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (especial atención a byte[])")
    void testEqualsAndHashCode() {
        byte[] data1 = {1, 2, 3};
        byte[] data2 = {1, 2, 3};
        byte[] data3 = {4, 5, 6};

        CarOfferResource r1 = new CarOfferResource().id(1).compressedResource(data1);
        CarOfferResource r2 = new CarOfferResource().id(1).compressedResource(data2);
        CarOfferResource r3 = new CarOfferResource().id(1).compressedResource(data3);

        // Identidad (this == o) -> Cubre return true
        assertThat(r1.equals(r1)).isTrue();

        // Igualdad por contenido de array (Arrays.equals)
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());

        // Diferencia por contenido de array
        assertThat(r1).isNotEqualTo(r3);

        // Casos de error/ramas false
        assertThat(r1).isNotEqualTo(null);
        assertThat(r1).isNotEqualTo(new Object());
        assertThat(r1).isNotEqualTo(new CarOfferResource().id(2));
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToString() {
        CarOfferResource model = new CarOfferResource().id(10).resource("res");

        // Cubre toString y toIndentedString con valores
        String result = model.toString();
        assertThat(result).contains("id: 10");
        assertThat(result).contains("resource: res");

        // Cubre toIndentedString con nulls
        CarOfferResource empty = new CarOfferResource();
        assertThat(empty.toString()).contains("type: null");
    }
}
