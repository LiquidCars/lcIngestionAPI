package net.liquidcars.ingestion.infra.input.rest.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

public class MoneyTest {

    @Test
    @DisplayName("Cobertura total: Constructores, Setters y Getters")
    void testConstructorsAndAccessors() {
        // 1. Probar constructor vacío (super() implícito)
        Money moneyEmpty = new Money();
        assertThat(moneyEmpty).isNotNull();

        // 2. Probar constructor con parámetros (Campos requeridos)
        BigDecimal amount = new BigDecimal("15000.50");
        String currency = "EUR";
        Money moneyParam = new Money(amount, currency);

        assertThat(moneyParam.getAmount()).isEqualTo(amount);
        assertThat(moneyParam.getCurrency()).isEqualTo(currency);

        // 3. Probar métodos fluídos (Builder style)
        Money moneyFluent = new Money()
                .amount(new BigDecimal("100"))
                .currency("USD");

        // 4. Probar setters tradicionales
        moneyFluent.setAmount(new BigDecimal("200"));
        moneyFluent.setCurrency("GBP");

        // Verificación final
        assertThat(moneyFluent.getAmount()).isEqualTo(new BigDecimal("200"));
        assertThat(moneyFluent.getCurrency()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("Cobertura total de Equals y HashCode")
    void testEqualsAndHashCode() {
        Money m1 = new Money(new BigDecimal("10"), "EUR");
        Money m2 = new Money(new BigDecimal("10"), "EUR");
        Money m3 = new Money(new BigDecimal("20"), "USD");

        // Identidad (this == o)
        assertThat(m1.equals(m1)).isTrue();

        // Valores iguales
        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());

        // Diferencias y casos nulos
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(null);
        assertThat(m1).isNotEqualTo(new Object());
    }

    @Test
    @DisplayName("Cobertura de toString e indentación")
    void testToString() {
        Money money = new Money(new BigDecimal("50"), "EUR");

        // Cubre toString y toIndentedString con datos
        String result = money.toString();
        assertThat(result).contains("amount: 50");
        assertThat(result).contains("currency: EUR");

        // Cubre toIndentedString cuando un valor es null
        Money empty = new Money();
        assertThat(empty.toString()).contains("amount: null");
    }
}
