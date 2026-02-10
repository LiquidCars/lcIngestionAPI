package net.liquidcars.ingestion.infra.input.rest.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import net.liquidcars.ingestion.factory.OfferRequestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
public class OfferRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }


    @Test
    @DisplayName("Constraint validation: Minimum price")
    void shouldFailWhenPriceIsTooLow() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();
        request.getPrice().setAmount(new BigDecimal("0.09"));

        Set<ConstraintViolation<OfferRequest>> violations = validator.validate(request);

        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("price") &&
                        v.getConstraintDescriptor().getAnnotation().annotationType().equals(jakarta.validation.constraints.DecimalMin.class)
        );
    }


    @Test
    @DisplayName("Full coverage of Equals and toIndentedString")
    @SuppressWarnings("all")
    void testEqualsComplexRamas() {
        OfferRequest req1 = OfferRequestFactory.getOfferRequest();
        OfferRequest req2 = OfferRequestFactory.getOfferRequest();

        assertThat(req1.equals(req1)).isTrue();

        assertThat(req1.equals(null)).isFalse();

        assertThat(req1.equals("any_string")).isFalse();

        req2.setId(UUID.randomUUID());
        assertThat(req1.equals(req2)).isFalse();

        OfferRequest reqNull = new OfferRequest();
        assertThat(reqNull.toString()).isNotNull();
    }
}
