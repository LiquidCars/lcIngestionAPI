package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GPSLocationTest {

    @Test
    @DisplayName("Cobertura total: Acceso a datos (Setters, Getters y Métodos Fluídos)")
    void testAccessorsAndFluentApi() {
        GPSLocation gps = new GPSLocation();

        // 1. Métodos fluídos (Estilo Builder, los que no tienen prefijo 'set')
        gps.name("Sede Central")
                .longitude(-3.70379)
                .latitude(40.41678);

        // 2. Setters tradicionales (Para asegurar 100% de cobertura en métodos 'setXXX')
        gps.setName("Nueva Ubicación");
        gps.setLongitude(2.1734);
        gps.setLatitude(41.3851);

        // 3. Verificación con Getters
        assertThat(gps.getName()).isEqualTo("Nueva Ubicación");
        assertThat(gps.getLongitude()).isEqualTo(2.1734);
        assertThat(gps.getLatitude()).isEqualTo(41.3851);
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (Todas las ramas)")
    void testEqualsAndHashCode() {
        GPSLocation gps1 = new GPSLocation().name("Madrid").latitude(40.0);
        GPSLocation gps2 = new GPSLocation().name("Madrid").latitude(40.0);
        GPSLocation gps3 = new GPSLocation().name("Barcelona").latitude(41.0);

        // Identidad (this == o) -> Cubre el 'return true' inicial del equals
        assertThat(gps1.equals(gps1)).isTrue();

        // Comparación de valores (Simetría)
        assertThat(gps1).isEqualTo(gps2);
        assertThat(gps1.hashCode()).isEqualTo(gps2.hashCode());

        // Diferencias y casos nulos (Ramas de return false)
        assertThat(gps1).isNotEqualTo(gps3);
        assertThat(gps1).isNotEqualTo(null);
        assertThat(gps1).isNotEqualTo(new Object());
        assertThat(gps1.hashCode()).isNotEqualTo(gps3.hashCode());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToStringAndIndentation() {
        GPSLocation gps = new GPSLocation().name("Punto A").longitude(10.5);

        // Cubre toString y toIndentedString con valores presentes
        String result = gps.toString();
        assertThat(result).contains("name: Punto A");
        assertThat(result).contains("longitude: 10.5");

        // Cubre toIndentedString cuando un campo es null
        GPSLocation emptyGps = new GPSLocation();
        assertThat(emptyGps.toString()).contains("latitude: null");
    }
}
