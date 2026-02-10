package net.liquidcars.ingestion.domain.model;

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
@Schema(title="The Money DTO object", description="A representation of money")
@Value
@JsonPropertyOrder({"amount", "currency"})
public class MoneyDto implements Serializable, Comparable<MoneyDto> {

    @Schema(description = "The money amount")
    BigDecimal amount;
    @Schema(description = "The currency code")
    String currency;

    @Builder
    public MoneyDto(@JsonProperty("amount") BigDecimal amount,
                    @JsonProperty("currency") String currency) {
        if (amount != null) {
            this.amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        } else {
            this.amount = null;
        }
        this.currency = currency;
    }

    public static MoneyDto toMoney(double amount, String currencyId) {
        BigDecimal value = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(value, currencyId);
    }

    public static MoneyDto toMoney(BigDecimal amount, String currencyId) {
        if (amount == null) return new MoneyDto(null, currencyId);
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(value, currencyId);
    }

    public static MoneyDto toMoney(long amount, String currencyId) {
        BigDecimal value = BigDecimal.valueOf(amount)
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(value, currencyId);
    }

    public static MoneyDto fromMoney(MoneyDto source)
    {
        return new MoneyDto(source.getAmount(), source.getCurrency());
    }


    public MoneyDto plus(MoneyDto amount) {
        if (!this.getCurrency().equals(amount.getCurrency())) return null;
        BigDecimal result = this.getAmount().add(amount.getAmount())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(result, this.getCurrency());
    }

    public MoneyDto minus(MoneyDto amount) {
        if (!this.getCurrency().equals(amount.getCurrency())) return null;
        BigDecimal result = this.getAmount().subtract(amount.getAmount())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(result, this.getCurrency());
    }

    public MoneyDto multiply(double multiplier) {
        BigDecimal result = this.getAmount()
                .multiply(BigDecimal.valueOf(multiplier))
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(result, this.getCurrency());
    }

    public MoneyDto multiply(long amount) {
        BigDecimal result = this.getAmount()
                .multiply(BigDecimal.valueOf(amount))
                .setScale(2, RoundingMode.HALF_EVEN);
        return new MoneyDto(result, this.getCurrency());
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

    public boolean isComparableWith(MoneyDto amount){
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

    public int compareToExtended(MoneyDto amount){
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
        if (!(o instanceof MoneyDto)) {
            return false;
        }

        // typecast o to Money so that we can compare data members
        MoneyDto c = (MoneyDto) o;

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
    public int compareTo(MoneyDto amount) {
        if (!isComparableWith(amount)) {
            return -2; // Different currencies, not comparable
        }
        return this.getAmount().compareTo(amount.getAmount());
    }
}
