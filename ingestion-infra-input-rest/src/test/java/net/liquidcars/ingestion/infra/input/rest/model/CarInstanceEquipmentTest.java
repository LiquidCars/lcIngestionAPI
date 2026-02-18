package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class CarInstanceEquipmentTest {

    @Test
    @DisplayName("Cobertura total: Acceso a datos (Setters, Getters y Fluídos)")
    void testAccessorsAndFluentApi() {
        CarInstanceEquipment model = new CarInstanceEquipment();

        // Objetos de apoyo
        KeyValue kv = new KeyValue();
        Money price = new Money();

        // 1. Métodos fluídos (Sin prefijo 'set')
        model.id(10)
                .equipment(kv)
                .category(kv)
                .type(kv)
                .description("Pack Luces LED")
                .code("LED-001")
                .price(price);

        // 2. Setters tradicionales (Para Jacoco/Cobertura)
        model.setId(20);
        model.setEquipment(kv);
        model.setCategory(kv);
        model.setType(kv);
        model.setDescription("Actualizado");
        model.setCode("CODE-99");
        model.setPrice(price);

        // 3. Verificación con Getters
        assertThat(model.getId()).isEqualTo(20);
        assertThat(model.getEquipment()).isEqualTo(kv);
        assertThat(model.getCategory()).isEqualTo(kv);
        assertThat(model.getType()).isEqualTo(kv);
        assertThat(model.getDescription()).isEqualTo("Actualizado");
        assertThat(model.getCode()).isEqualTo("CODE-99");
        assertThat(model.getPrice()).isEqualTo(price);
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (Todas las ramas)")
    void testEqualsAndHashCode() {
        CarInstanceEquipment e1 = new CarInstanceEquipment().id(1).code("X");
        CarInstanceEquipment e2 = new CarInstanceEquipment().id(1).code("X");
        CarInstanceEquipment e3 = new CarInstanceEquipment().id(2).code("Y");

        // Identidad (this == o) -> Cubre return true inicial
        assertThat(e1.equals(e1)).isTrue();

        // Comparación de valores
        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());

        // Diferencias y casos nulos (Ramas de return false)
        assertThat(e1).isNotEqualTo(e3);
        assertThat(e1).isNotEqualTo(null);
        assertThat(e1).isNotEqualTo(new Object());
        assertThat(e1.hashCode()).isNotEqualTo(e3.hashCode());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToStringAndIndentation() {
        CarInstanceEquipment model = new CarInstanceEquipment().id(5).description("Test");

        // Cubre toString y toIndentedString con valores
        String result = model.toString();
        assertThat(result).contains("id: 5");
        assertThat(result).contains("description: Test");

        // Cubre toIndentedString cuando el objeto es null
        CarInstanceEquipment emptyModel = new CarInstanceEquipment();
        assertThat(emptyModel.toString()).contains("code: null");
    }
}
