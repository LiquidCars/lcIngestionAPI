package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostalAddressTest {

    @Test
    @DisplayName("Cobertura total: Setters, Métodos Fluídos y Getters")
    void fullCoverageTest() {
        PostalAddress address = new PostalAddress();
        GPSLocation gps = new GPSLocation(); // Asumiendo que existe en el mismo paquete

        // 1. PROBAMOS MÉTODOS FLUÍDOS (Para cubrir los métodos sin el prefijo 'set')
        address.name("Oficina Central")
                .gpsLocation(gps)
                .streetNumber("123")
                .streetAddress("Calle Falsa")
                .extendedAddress("Bloque B, Planta 4")
                .postalCode("28001")
                .city("Madrid")
                .region("Madrid")
                .country("España")
                .countryCode("ES")
                .poBox("Apdo 500")
                .type("Business");

        // 2. PROBAMOS SETTERS ESTÁNDAR (Para cubrir los métodos 'setXXX')
        address.setName("Nueva Oficina");
        address.setGpsLocation(gps);
        address.setStreetNumber("456");
        address.setStreetAddress("Avenida Principal");
        address.setExtendedAddress("Puerta A");
        address.setPostalCode("08001");
        address.setCity("Barcelona");
        address.setRegion("Cataluña");
        address.setCountry("Spain");
        address.setCountryCode("ESP");
        address.setPoBox("PO BOX 10");
        address.setType("Home");

        // 3. VERIFICACIÓN DE GETTERS
        assertThat(address.getName()).isEqualTo("Nueva Oficina");
        assertThat(address.getGpsLocation()).isEqualTo(gps);
        assertThat(address.getStreetNumber()).isEqualTo("456");
        assertThat(address.getStreetAddress()).isEqualTo("Avenida Principal");
        assertThat(address.getExtendedAddress()).isEqualTo("Puerta A");
        assertThat(address.getPostalCode()).isEqualTo("08001");
        assertThat(address.getCity()).isEqualTo("Barcelona");
        assertThat(address.getRegion()).isEqualTo("Cataluña");
        assertThat(address.getCountry()).isEqualTo("Spain");
        assertThat(address.getCountryCode()).isEqualTo("ESP");
        assertThat(address.getPoBox()).isEqualTo("PO BOX 10");
        assertThat(address.getType()).isEqualTo("Home");
    }

    @Test
    @DisplayName("Cobertura total de equals, hashCode y toString")
    void testLogicMethods() {
        PostalAddress a1 = new PostalAddress().name("Casa").city("Madrid");
        PostalAddress a2 = new PostalAddress().name("Casa").city("Madrid");
        PostalAddress a3 = new PostalAddress().name("Trabajo").city("Valencia");

        // --- EQUALS ---
        // Identidad (this == o) -> Cubre el 'return true' inicial
        assertThat(a1).isEqualTo(a1);

        // Simetría y valores
        assertThat(a1).isEqualTo(a2);

        // Casos negativos (Ramas de 'return false')
        assertThat(a1).isNotEqualTo(a3);
        assertThat(a1).isNotEqualTo(null);
        assertThat(a1).isNotEqualTo("No soy una dirección");

        // --- HASHCODE ---
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
        assertThat(a1.hashCode()).isNotEqualTo(a3.hashCode());

        // --- TOSTRING ---
        String result = a1.toString();
        assertThat(result).contains("class PostalAddress");
        assertThat(result).contains("name: Casa");
        assertThat(result).contains("city: Madrid");

        // Probar toString con valores nulos para cubrir 'toIndentedString' con nulos
        assertThat(new PostalAddress().toString()).contains("name: null");
    }
}
