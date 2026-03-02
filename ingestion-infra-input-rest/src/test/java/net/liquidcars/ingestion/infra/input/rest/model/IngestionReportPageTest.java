package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IngestionReportPageTest {

    @Test
    @DisplayName("Cobertura total: Setters, Getters y Métodos Fluídos")
    void testAccessorsAndFluentApi() {
        IngestionReportPage page = new IngestionReportPage();
        List<IngestionReport> content = new ArrayList<>();
        content.add(new IngestionReport());

        // 1. Métodos fluídos (estilo Builder)
        page.content(content)
                .totalElements(100L)
                .totalPages(10)
                .size(10)
                .number(0)
                .last(false);

        // 2. Setters tradicionales (para cobertura de Jacoco)
        page.setContent(content);
        page.setTotalElements(50L);
        page.setTotalPages(5);
        page.setSize(10);
        page.setNumber(1);
        page.setLast(true);

        // 3. Verificación con Getters
        assertThat(page.getContent()).isEqualTo(content);
        assertThat(page.getTotalElements()).isEqualTo(50L);
        assertThat(page.getTotalPages()).isEqualTo(5);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getLast()).isTrue();
    }

    @Test
    @DisplayName("Cobertura de lógica de lista (addContentItem con null)")
    void testAddContentItemNullSafety() {
        IngestionReportPage page = new IngestionReportPage();

        // Forzamos que la lista sea null para entrar en el 'if (this.content == null)'
        page.setContent(null);

        IngestionReport report = new IngestionReport();
        page.addContentItem(report);

        assertThat(page.getContent()).isNotNull().hasSize(1).contains(report);
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (todas las ramas)")
    void testEqualsAndHashCode() {
        IngestionReportPage p1 = new IngestionReportPage().totalElements(10L).size(5);
        IngestionReportPage p2 = new IngestionReportPage().totalElements(10L).size(5);
        IngestionReportPage p3 = new IngestionReportPage().totalElements(20L).size(5);

        // Identidad (this == o)
        assertThat(p1.equals(p1)).isTrue();

        // Igualdad por valor
        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());

        // Desigualdades y nulos
        assertThat(p1).isNotEqualTo(p3);
        assertThat(p1).isNotEqualTo(null);
        assertThat(p1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToStringAndIndentation() {
        IngestionReportPage page = new IngestionReportPage().totalElements(100L);

        // Cubre toString y toIndentedString con valores
        String result = page.toString();
        assertThat(result).contains("totalElements: 100");

        // Cubre toIndentedString cuando un campo es null
        IngestionReportPage empty = new IngestionReportPage();
        assertThat(empty.toString()).contains("content: []"); // ArrayList por defecto

        empty.setContent(null); // Forzamos null para cubrir el if del método privado
        assertThat(empty.toString()).contains("content: null");
    }
}
