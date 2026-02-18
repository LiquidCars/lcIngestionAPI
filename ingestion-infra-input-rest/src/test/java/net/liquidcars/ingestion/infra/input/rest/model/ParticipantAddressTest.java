package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ParticipantAddressTest {

    @Test
    @DisplayName("Cobertura total: Setters, Getters y Métodos Fluídos")
    void testAccessorsAndFluentApi() {
        ParticipantAddress participantAddress = new ParticipantAddress();

        // Objetos de apoyo
        AddressType type = AddressType.B_PICKUP; // Asumiendo que existe en el paquete
        PostalAddress address = new PostalAddress();

        // 1. Métodos fluídos (Estilo Builder)
        participantAddress.type(type)
                .address(address);

        // 2. Setters tradicionales (Para asegurar 100% en Jacoco)
        participantAddress.setType(type);
        participantAddress.setAddress(address);

        // 3. Verificación con Getters
        assertThat(participantAddress.getType()).isEqualTo(type);
        assertThat(participantAddress.getAddress()).isEqualTo(address);
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode (Todas las ramas)")
    void testEqualsAndHashCode() {
        AddressType type = AddressType.B_PICKUP;
        PostalAddress address = new PostalAddress();

        ParticipantAddress pa1 = new ParticipantAddress().type(type).address(address);
        ParticipantAddress pa2 = new ParticipantAddress().type(type).address(address);
        ParticipantAddress pa3 = new ParticipantAddress().type(type).address(null);

        // Identidad (this == o) -> Cubre return true inicial
        assertThat(pa1.equals(pa1)).isTrue();

        // Comparación de valores
        assertThat(pa1).isEqualTo(pa2);
        assertThat(pa1.hashCode()).isEqualTo(pa2.hashCode());

        // Diferencias y casos nulos (Ramas de return false)
        assertThat(pa1).isNotEqualTo(pa3);
        assertThat(pa1).isNotEqualTo(null);
        assertThat(pa1).isNotEqualTo(new Object());
        assertThat(pa1.hashCode()).isNotEqualTo(pa3.hashCode());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToStringAndIndentation() {
        ParticipantAddress pa = new ParticipantAddress().type(AddressType.B_PICKUP);

        // Cubre toString y toIndentedString con valores
        String result = pa.toString();
        assertThat(result).contains("class ParticipantAddress");
        assertThat(result).contains("type:");

        // Cubre toIndentedString con nulls para el 100% de la lógica interna
        ParticipantAddress empty = new ParticipantAddress();
        assertThat(empty.toString()).contains("address: null");
    }
}
