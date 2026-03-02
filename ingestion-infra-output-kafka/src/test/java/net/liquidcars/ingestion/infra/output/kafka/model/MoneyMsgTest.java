package net.liquidcars.ingestion.infra.output.kafka.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MoneyMsgTest {

    @Spy
    private MoneyMsg moneySpy = MoneyMsg.toMoney(100.0, "EUR");

    @Test
    @DisplayName("Constructor y Builder: Deberían aplicar redondeo HALF_EVEN")
    void constructorAndBuilderTest() {
        MoneyMsg fromBuilder = MoneyMsg.builder()
                .amount(new BigDecimal("10.555")) // Debería redondear a 10.56
                .currency("USD")
                .build();

        assertThat(fromBuilder.getAmount()).isEqualTo(new BigDecimal("10.56"));

        MoneyMsg nullAmount = new MoneyMsg(null, "EUR");
        assertThat(nullAmount.getAmount()).isNull();
    }

    @Test
    @DisplayName("Métodos toMoney: Cobertura de todas las sobrecargas")
    void toMoneyFactoriesTest() {
        assertThat(MoneyMsg.toMoney(10.0, "EUR").getAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(MoneyMsg.toMoney(10L, "EUR").getAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(MoneyMsg.toMoney((BigDecimal) null, "EUR").getAmount()).isNull();

        MoneyMsg original = MoneyMsg.toMoney(50, "EUR");
        MoneyMsg cloned = MoneyMsg.fromMoney(original);
        assertThat(cloned).isEqualTo(original);
    }

    @Test
    @DisplayName("toString: Cobertura de flujos con datos y con nulos")
    void toStringTest() {
        MoneyMsg normal = MoneyMsg.toMoney(1234.56, "EUR");
        assertThat(normal.toString()).isEqualTo("1,234.56 EUR");

        assertThat(new MoneyMsg(null, "EUR").toString()).isEqualTo("null");
        assertThat(new MoneyMsg(new BigDecimal("10"), null).toString()).isEqualTo("null");
    }

    @Test
    @DisplayName("hashCode: Cobertura de ramas con nulos")
    void hashCodeTest() {
        MoneyMsg m1 = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg m2 = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg mNulls = new MoneyMsg(null, null);

        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1.hashCode()).isNotEqualTo(mNulls.hashCode());
        assertThat(mNulls.hashCode()).isNotNull();
    }

    @Test
    @DisplayName("equals: Cobertura de todas las condiciones lógicas")
    void equalsTest() {
        MoneyMsg m1 = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg m2 = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg mDiff = MoneyMsg.toMoney(20, "EUR");
        MoneyMsg mDiffCurr = MoneyMsg.toMoney(10, "USD");

        assertThat(m1.equals(m1)).isTrue();
        assertThat(m1.equals(m2)).isTrue();
        assertThat(m1.equals(null)).isFalse();
        assertThat(m1.equals("String")).isFalse();
        assertThat(m1.equals(mDiff)).isFalse();
        assertThat(m1.equals(mDiffCurr)).isFalse();
    }

    @Test
    @DisplayName("Aritmética: plus, minus, multiply")
    void arithmeticTest() {
        MoneyMsg m1 = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg m2 = MoneyMsg.toMoney(5, "EUR");
        MoneyMsg mUSD = MoneyMsg.toMoney(5, "USD");

        assertThat(m1.plus(m2).getAmount()).isEqualTo(new BigDecimal("15.00"));
        assertThat(m1.plus(mUSD)).isNull();

        assertThat(m1.minus(m2).getAmount()).isEqualTo(new BigDecimal("5.00"));
        assertThat(m1.minus(mUSD)).isNull();

        assertThat(m1.multiply(2.0).getAmount()).isEqualTo(new BigDecimal("20.00"));
        assertThat(m1.multiply(3L).getAmount()).isEqualTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("Lógica de negocio: isPositive, compareToExtended")
    void businessLogicTest() {
        assertThat(MoneyMsg.toMoney(0.01, "EUR").isPositive()).isTrue();
        assertThat(MoneyMsg.toMoney(0, "EUR").isPositive()).isFalse();

        MoneyMsg eur = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg usd = MoneyMsg.toMoney(10, "USD");

        assertThat(eur.compareToExtended(usd)).isEqualTo(-2);
        assertThat(eur.isComparableWith(MoneyMsg.toMoney(10, "eur"))).isTrue();
    }

    @Test
    @DisplayName("Mockito Spy: Verificar que compareTo llama a isComparableWith")
    void spyTest() {
        MoneyMsg other = MoneyMsg.toMoney(10, "USD");

        int result = moneySpy.compareTo(other);

        assertThat(result).isEqualTo(-2);
        verify(moneySpy).isComparableWith(other);
    }

    @Test
    @DisplayName("toMoney: Debe cubrir todas las sobrecargas y tipos de datos")
    void toMoneyComprehensiveTest() {
        MoneyMsg mDouble = MoneyMsg.toMoney(10.555, "EUR");
        assertThat(mDouble.getAmount()).isEqualTo(new BigDecimal("10.56")); // HALF_EVEN

        MoneyMsg mLong = MoneyMsg.toMoney(100L, "USD");
        assertThat(mLong.getAmount()).isEqualTo(new BigDecimal("100.00"));

        MoneyMsg mBig = MoneyMsg.toMoney(new BigDecimal("50.1"), "GBP");
        assertThat(mBig.getAmount()).isEqualTo(new BigDecimal("50.10"));

        MoneyMsg mNull = MoneyMsg.toMoney((BigDecimal) null, "JPY");
        assertThat(mNull.getAmount()).isNull();
        assertThat(mNull.getCurrency()).isEqualTo("JPY");
    }

    @Test
    @DisplayName("compareTo y compareToExtended: Cobertura de lógica de comparación")
    void comparisonDeepTest() {
        MoneyMsg eur10 = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg eur20 = MoneyMsg.toMoney(20, "EUR");
        MoneyMsg usd10 = MoneyMsg.toMoney(10, "USD");

        assertThat(eur10.compareTo(eur20)).isNegative();
        assertThat(eur20.compareTo(eur10)).isPositive();
        assertThat(eur10.compareTo(eur10)).isZero();
        assertThat(eur10.compareTo(usd10)).isEqualTo(-2);

        assertThat(eur10.compareToExtended(usd10)).isEqualTo(-2);
        assertThat(eur10.compareToExtended(eur10)).isZero();
    }

    @Test
    @DisplayName("Mockito Spy: Verificar delegación interna de compareTo")
    void verifyInternalCalls() {
        MoneyMsg base = MoneyMsg.toMoney(10, "EUR");
        MoneyMsg spyMoney = spy(base);
        MoneyMsg other = MoneyMsg.toMoney(10, "USD");

        int result = spyMoney.compareTo(other);

        assertThat(result).isEqualTo(-2);

        verify(spyMoney).isComparableWith(other);
    }
}
