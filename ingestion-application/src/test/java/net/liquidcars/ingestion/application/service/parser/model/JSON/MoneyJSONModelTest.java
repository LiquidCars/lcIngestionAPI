package net.liquidcars.ingestion.application.service.parser.model.JSON;

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
public class MoneyJSONModelTest {

    @Spy
    private MoneyJSONModel moneySpy = MoneyJSONModel.toMoney(100.0, "EUR");

    @Test
    @DisplayName("Constructor y Builder: Deberían aplicar redondeo HALF_EVEN")
    void constructorAndBuilderTest() {
        MoneyJSONModel fromBuilder = MoneyJSONModel.builder()
                .amount(new BigDecimal("10.555")) // Debería redondear a 10.56
                .currency("USD")
                .build();

        assertThat(fromBuilder.getAmount()).isEqualTo(new BigDecimal("10.56"));

        MoneyJSONModel nullAmount = new MoneyJSONModel(null, "EUR");
        assertThat(nullAmount.getAmount()).isNull();
    }

    @Test
    @DisplayName("Métodos toMoney: Cobertura de todas las sobrecargas")
    void toMoneyFactoriesTest() {
        assertThat(MoneyJSONModel.toMoney(10.0, "EUR").getAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(MoneyJSONModel.toMoney(10L, "EUR").getAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(MoneyJSONModel.toMoney((BigDecimal) null, "EUR").getAmount()).isNull();

        MoneyJSONModel original = MoneyJSONModel.toMoney(50, "EUR");
        MoneyJSONModel cloned = MoneyJSONModel.fromMoney(original);
        assertThat(cloned).isEqualTo(original);
    }

    @Test
    @DisplayName("toString: Cobertura de flujos con datos y con nulos")
    void toStringTest() {
        MoneyJSONModel normal = MoneyJSONModel.toMoney(1234.56, "EUR");
        assertThat(normal.toString()).isEqualTo("1,234.56 EUR");

        assertThat(new MoneyJSONModel(null, "EUR").toString()).isEqualTo("null");
        assertThat(new MoneyJSONModel(new BigDecimal("10"), null).toString()).isEqualTo("null");
    }

    @Test
    @DisplayName("hashCode: Cobertura de ramas con nulos")
    void hashCodeTest() {
        MoneyJSONModel m1 = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel m2 = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel mNulls = new MoneyJSONModel(null, null);

        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1.hashCode()).isNotEqualTo(mNulls.hashCode());
        assertThat(mNulls.hashCode()).isNotNull();
    }

    @Test
    @DisplayName("equals: Cobertura de todas las condiciones lógicas")
    void equalsTest() {
        MoneyJSONModel m1 = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel m2 = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel mDiff = MoneyJSONModel.toMoney(20, "EUR");
        MoneyJSONModel mDiffCurr = MoneyJSONModel.toMoney(10, "USD");

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
        MoneyJSONModel m1 = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel m2 = MoneyJSONModel.toMoney(5, "EUR");
        MoneyJSONModel mUSD = MoneyJSONModel.toMoney(5, "USD");

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
        assertThat(MoneyJSONModel.toMoney(0.01, "EUR").isPositive()).isTrue();
        assertThat(MoneyJSONModel.toMoney(0, "EUR").isPositive()).isFalse();

        MoneyJSONModel eur = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel usd = MoneyJSONModel.toMoney(10, "USD");

        assertThat(eur.compareToExtended(usd)).isEqualTo(-2);
        assertThat(eur.isComparableWith(MoneyJSONModel.toMoney(10, "eur"))).isTrue();
    }

    @Test
    @DisplayName("Mockito Spy: Verificar que compareTo llama a isComparableWith")
    void spyTest() {
        MoneyJSONModel other = MoneyJSONModel.toMoney(10, "USD");

        int result = moneySpy.compareTo(other);

        assertThat(result).isEqualTo(-2);
        verify(moneySpy).isComparableWith(other);
    }

    @Test
    @DisplayName("toMoney: Debe cubrir todas las sobrecargas y tipos de datos")
    void toMoneyComprehensiveTest() {
        MoneyJSONModel mDouble = MoneyJSONModel.toMoney(10.555, "EUR");
        assertThat(mDouble.getAmount()).isEqualTo(new BigDecimal("10.56")); // HALF_EVEN

        MoneyJSONModel mLong = MoneyJSONModel.toMoney(100L, "USD");
        assertThat(mLong.getAmount()).isEqualTo(new BigDecimal("100.00"));

        MoneyJSONModel mBig = MoneyJSONModel.toMoney(new BigDecimal("50.1"), "GBP");
        assertThat(mBig.getAmount()).isEqualTo(new BigDecimal("50.10"));

        MoneyJSONModel mNull = MoneyJSONModel.toMoney((BigDecimal) null, "JPY");
        assertThat(mNull.getAmount()).isNull();
        assertThat(mNull.getCurrency()).isEqualTo("JPY");
    }

    @Test
    @DisplayName("compareTo y compareToExtended: Cobertura de lógica de comparación")
    void comparisonDeepTest() {
        MoneyJSONModel eur10 = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel eur20 = MoneyJSONModel.toMoney(20, "EUR");
        MoneyJSONModel usd10 = MoneyJSONModel.toMoney(10, "USD");

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
        MoneyJSONModel base = MoneyJSONModel.toMoney(10, "EUR");
        MoneyJSONModel spyMoney = spy(base);
        MoneyJSONModel other = MoneyJSONModel.toMoney(10, "USD");

        int result = spyMoney.compareTo(other);

        assertThat(result).isEqualTo(-2);

        verify(spyMoney).isComparableWith(other);
    }
}
