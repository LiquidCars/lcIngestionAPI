package net.liquidcars.ingestion.infra.postgresql.mapper;

import net.liquidcars.ingestion.infra.postgresql.service.mapper.JsonStringListConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonStringListConverterTest {

    private JsonStringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonStringListConverter();
    }


    @Test
    @DisplayName("Debe convertir una lista válida a un string JSON")
    void convertToDatabaseColumn_ShouldReturnJsonString() {
        List<String> list = List.of("A", "B", "C");
        String result = converter.convertToDatabaseColumn(list);
        assertThat(result).isEqualTo("[\"A\",\"B\",\"C\"]");
    }

    @Test
    @DisplayName("Debe retornar [] si la lista es nula")
    void convertToDatabaseColumn_ShouldReturnEmptyJsonArray_WhenListIsNull() {
        String result = converter.convertToDatabaseColumn(null);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    @DisplayName("Debe retornar [] si ocurre una excepción de serialización")
    void convertToDatabaseColumn_ShouldHandleException() {
        String result = converter.convertToDatabaseColumn(null);
        assertThat(result).isEqualTo("[]");
    }


    @Test
    @DisplayName("Debe convertir un JSON válido a una lista de Strings")
    void convertToEntityAttribute_ShouldReturnList() {
        String dbData = "[\"Java\",\"Spring\"]";
        List<String> result = converter.convertToEntityAttribute(dbData);
        assertThat(result).containsExactly("Java", "Spring");
    }

    @ParameterizedTest
    @DisplayName("Debe retornar lista vacía si el string es nulo o vacío")
    @NullSource
    @ValueSource(strings = {"", " "})
    void convertToEntityAttribute_ShouldReturnEmptyList_WhenDataIsNullOrEmpty(String input) {
        List<String> result = converter.convertToEntityAttribute(input);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe retornar lista vacía y capturar error si el JSON es inválido")
    void convertToEntityAttribute_ShouldReturnEmptyList_OnInvalidJson() {
        String invalidJson = "{ esto no es un array }";
        List<String> result = converter.convertToEntityAttribute(invalidJson);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("convertToDatabaseColumn: Debe retornar [] y loguear error si falla la serialización")
    void convertToDatabaseColumn_ShouldReturnEmptyArray_OnException() {
        List problematicList = new java.util.ArrayList();
        problematicList.add(new Object() {
            public String getProp() {
                throw new RuntimeException("Jackson fail");
            }
        });

        String result = converter.convertToDatabaseColumn((List<String>) problematicList);

        assertThat(result).isEqualTo("[]");
    }

}
