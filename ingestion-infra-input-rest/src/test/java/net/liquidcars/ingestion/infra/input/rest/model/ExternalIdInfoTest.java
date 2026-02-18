package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ExternalIdInfoTest {

    @Test
    @DisplayName("Cobertura total: Setters, Getters y Métodos Fluídos")
    void testAccessorsAndFluentApi() {
        ExternalIdInfo info = new ExternalIdInfo();

        // 1. Métodos fluídos (estilo builder)
        info.ownerReference("OWN-123")
                .dealerReference("DEA-456")
                .channelReference("CHA-789");

        // 2. Setters tradicionales (para asegurar cobertura total en Jacoco)
        info.setOwnerReference("NEW-OWNER");
        info.setDealerReference("NEW-DEALER");
        info.setChannelReference("NEW-CHANNEL");

        // 3. Verificación con Getters
        assertThat(info.getOwnerReference()).isEqualTo("NEW-OWNER");
        assertThat(info.getDealerReference()).isEqualTo("NEW-DEALER");
        assertThat(info.getChannelReference()).isEqualTo("NEW-CHANNEL");
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (todas las ramas)")
    void testEqualsAndHashCode() {
        ExternalIdInfo info1 = new ExternalIdInfo().ownerReference("REF1").dealerReference("DEA1");
        ExternalIdInfo info2 = new ExternalIdInfo().ownerReference("REF1").dealerReference("DEA1");
        ExternalIdInfo info3 = new ExternalIdInfo().ownerReference("REF2").dealerReference("DEA2");

        // Identidad (this == o) -> Cubre el return true inicial
        assertThat(info1.equals(info1)).isTrue();

        // Comparación de valores (Simetría e igualdad)
        assertThat(info1).isEqualTo(info2);
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());

        // Diferencias y casos nulos (Ramas de return false)
        assertThat(info1).isNotEqualTo(info3);
        assertThat(info1).isNotEqualTo(null);
        assertThat(info1).isNotEqualTo(new Object());
        assertThat(info1.hashCode()).isNotEqualTo(info3.hashCode());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToStringAndIndentation() {
        ExternalIdInfo info = new ExternalIdInfo().ownerReference("VAL1");

        // Cubre toString y toIndentedString con valores presentes
        String result = info.toString();
        assertThat(result).contains("ownerReference: VAL1");

        // Cubre toIndentedString cuando un campo es null para asegurar 100% de cobertura
        ExternalIdInfo empty = new ExternalIdInfo();
        assertThat(empty.toString()).contains("dealerReference: null");
    }
}
