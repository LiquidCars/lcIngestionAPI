package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class KeyValueTest {

    @Test
    @DisplayName("Cobertura total: Setters, Getters y Métodos Fluídos")
    void testAccessorsAndFluentApi() {
        KeyValue kv = new KeyValue();

        // 1. Métodos fluídos (los que no tienen el prefijo 'set')
        kv.key("FUEL_TYPE")
                .value("Gasolina");

        // 2. Setters tradicionales (necesarios para el 100% de Jacoco)
        kv.setKey("COLOR");
        kv.setValue("Rojo");

        // 3. Verificación con Getters
        assertThat(kv.getKey()).isEqualTo("COLOR");
        assertThat(kv.getValue()).isEqualTo("Rojo");
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (todas las ramas)")
    void testEqualsAndHashCode() {
        KeyValue kv1 = new KeyValue().key("A").value("1");
        KeyValue kv2 = new KeyValue().key("A").value("1");
        KeyValue kv3 = new KeyValue().key("B").value("2");

        // Rama: Identidad (this == o) -> Cubre el 'return true' inicial
        assertThat(kv1.equals(kv1)).isTrue();

        // Rama: Mismos valores
        assertThat(kv1).isEqualTo(kv2);
        assertThat(kv1.hashCode()).isEqualTo(kv2.hashCode());

        // Ramas: Diferencias y casos nulos (return false)
        assertThat(kv1).isNotEqualTo(kv3);
        assertThat(kv1).isNotEqualTo(null);
        assertThat(kv1).isNotEqualTo(new Object());
        assertThat(kv1.hashCode()).isNotEqualTo(kv3.hashCode());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToStringAndIndentation() {
        KeyValue kv = new KeyValue().key("TEST_KEY").value("TEST_VALUE");

        // Cubre toString y toIndentedString con valores presentes
        String result = kv.toString();
        assertThat(result).contains("key: TEST_KEY");
        assertThat(result).contains("value: TEST_VALUE");

        // Cubre toIndentedString cuando un campo es null (rama del if o == null)
        KeyValue empty = new KeyValue();
        assertThat(empty.toString()).contains("key: null");
    }
}
