package net.liquidcars.ingestion.infra.output.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Data
@Schema(title="The Money Message object", description="A representation of money")
@Value
@JsonPropertyOrder({"amount", "currency"})
public class MoneyMsg implements Serializable, Comparable<MoneyMsg>{
    @Schema(description = "The money amount")
    BigDecimal amount;
    @Schema(description = "The currency code")
    String currency;

    @Builder
    public MoneyMsg(@JsonProperty("amount") BigDecimal amount,
                    @JsonProperty("currency") String currency) {
        if (amount != null) {
            this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        } else {
            this.amount = null;
        }
        this.currency = currency;
    }

    public static MoneyMsg toMoney(double amount, String currencyId) {
        BigDecimal value = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(value, currencyId);
    }

    public static MoneyMsg toMoney(BigDecimal amount, String currencyId) {
        if (amount == null) return new MoneyMsg(null, currencyId);
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(value, currencyId);
    }

    public static MoneyMsg toMoney(long amount, String currencyId) {
        BigDecimal value = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(value, currencyId);
    }

    public static MoneyMsg fromMoney(MoneyMsg source)
    {
        return new MoneyMsg(source.getAmount(), source.getCurrency());
    }


    public MoneyMsg plus(MoneyMsg amount) {
        if (!this.getCurrency().equals(amount.getCurrency())) return null;
        BigDecimal result = this.getAmount().add(amount.getAmount())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(result, this.getCurrency());
    }

    public MoneyMsg minus(MoneyMsg amount) {
        if (!this.getCurrency().equals(amount.getCurrency())) return null;
        BigDecimal result = this.getAmount().subtract(amount.getAmount())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(result, this.getCurrency());
    }

    public MoneyMsg multiply(double multiplier) {
        BigDecimal result = this.getAmount()
                .multiply(BigDecimal.valueOf(multiplier))
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(result, this.getCurrency());
    }

    public MoneyMsg multiply(long amount) {
        BigDecimal result = this.getAmount()
                .multiply(BigDecimal.valueOf(amount))
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyMsg(result, this.getCurrency());
    }


    // Overriding toString() method of String class
    @Override
    public String toString() {
        if(this.amount == null || this.currency == null)
            return "null";

        DecimalFormat df = new DecimalFormat("#,###.##", new DecimalFormatSymbols(Locale.ENGLISH));

        return df.format(this.amount) + " " + this.currency;
    }

    @JsonIgnore
    public boolean isPositive(){
        return getAmount().compareTo(BigDecimal.ZERO)>=1;
    }

    public boolean isComparableWith(MoneyMsg amount){
        return this.getCurrency().toUpperCase().equals(amount.getCurrency().toUpperCase());
    }

    /**
     * Compares this {@code Money} numerically with the specified
     * {@code Money}.  Two {@code Money} objects that are
     * equal in value and currency are considered equal.
     *
     * @apiNote
     * Note: this class has a natural ordering that is inconsistent with equals.
     *
     * @param  amount {@code Money} to which this {@code Money} is
     *         to be compared.
     * @return -1, 0, or 1 as this {@code BigDecimal} is numerically
     *          less than, equal to, or greater than {@code val}.-2 if both are not comparable due to having different currencies
     */

    public int compareToExtended(MoneyMsg amount){
        if(!isComparableWith(amount))
            return -2;
        return this.getAmount().compareTo(amount.getAmount());
    }

    // Overriding equals() to compare two Complex objects
    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /* Check if o is an instance of Complex or not
          "null instanceof [type]" also returns false */
        if (!(o instanceof MoneyMsg)) {
            return false;
        }

        // typecast o to Money so that we can compare data members
        MoneyMsg c = (MoneyMsg) o;

        // Compare the data members and return accordingly
        return compareToExtended(c) == 0;
    }

    @Override
    public int hashCode() {
        //Init numbers should be primes, and different for each HashCodeBuilder in different classes
        return Math.abs(new HashCodeBuilder(37,41)
                .append(amount !=null ? amount.hashCode() : 0).append(currency!=null ? currency.hashCode() : 0)
                .toHashCode());
    }

    @Override
    public int compareTo(MoneyMsg amount) {
        if (!isComparableWith(amount)) {
            return -2; // Different currencies, not comparable
        }
        return this.getAmount().compareTo(amount.getAmount());
    }
}
