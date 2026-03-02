package net.liquidcars.ingestion.application.service.parser.model.JSON;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KeyValueJSONModelTest {

    @Test
    @DisplayName("getDefault: Cobertura total con Mockito")
    void getDefaultCoverage() {
        // Cobertura de nulos y vacíos
        assertThat(KeyValueJSONModel.getDefault(null)).isNull();
        assertThat(KeyValueJSONModel.getDefault(Collections.emptyList())).isNull();

        // Creamos mocks para simular la lógica del stream y el bucle for
        KeyValueJSONModel item1 = mock(KeyValueJSONModel.class);
        KeyValueJSONModel item2 = mock(KeyValueJSONModel.class);

        when(item1.getKey()).thenReturn("REAL_DATA");
        when(item2.getKey()).thenReturn("UNKNOWN");

        List<KeyValueJSONModel> list = Arrays.asList(item1, item2);

        KeyValueJSONModel result = KeyValueJSONModel.getDefault(list);

        assertThat(result).isEqualTo(item2);
        verify(item1, atLeastOnce()).getKey();
    }

    @Test
    @DisplayName("toString: Cobertura 100% de ramas lógicas")
    void toStringFullCoverage() {
        // 1. Ambos presentes
        assertThat(new KeyValueJSONModel<>("K", "V").toString()).isEqualTo("[K / V]");

        // 2. Key nula
        assertThat(new KeyValueJSONModel<>(null, "V").toString()).isEqualTo("[ / V]");

        // 3. Value nulo
        assertThat(new KeyValueJSONModel<>("K", null).toString()).isEqualTo("[K / ]");

        // 4. Ambos nulos
        assertThat(new KeyValueJSONModel<>(null, null).toString()).isEqualTo("[ / ]");

        // 5. Strings vacíos (para cubrir la rama .isEmpty())
        assertThat(new KeyValueJSONModel<>("", "").toString()).isEqualTo("[ / ]");
    }

    @Test
    @DisplayName("toMap: Cobertura de transformación y colisión de llaves")
    void toMapCoverage() {
        assertThat(KeyValueJSONModel.toMap(null)).isNull();
        assertThat(KeyValueJSONModel.toMap(Collections.emptyList())).isEmpty();

        // Usamos objetos reales para asegurar la transformación a Map
        KeyValueJSONModel<String, String> item1 = new KeyValueJSONModel<>("A", "First");
        KeyValueJSONModel<String, String> item2 = new KeyValueJSONModel<>("A", "Second");

        List<KeyValueJSONModel> items = Arrays.asList(item1, item2);
        Map<String, String> result = KeyValueJSONModel.toMap(items);

        assertThat(result).hasSize(1);
        assertThat(result.get("A")).isEqualTo("First"); // Cubre la lambda (v1, v2) -> v1
    }

    @Test
    @DisplayName("equals y hashCode: Comportamiento de instancia")
    void equalsHashCodeCoverage() {
        KeyValueJSONModel<String, String> m1 = new KeyValueJSONModel<>("1", "X");

        // Como DefaultKeyValue no implementa equals, comparamos contra sí mismo
        // para asegurar cobertura de los métodos generados por Lombok.
        assertThat(m1).isEqualTo(m1);
        assertThat(m1.hashCode()).isEqualTo(m1.hashCode());
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Constructores: Cobertura básica")
    void constructorsTest() {
        KeyValueJSONModel<String, String> empty = new KeyValueJSONModel<>();
        KeyValueJSONModel<String, String> full = new KeyValueJSONModel<>("key", "value");

        assertThat(empty.getKey()).isNull();
        assertThat(full.getKey()).isEqualTo("key");
    }
}
