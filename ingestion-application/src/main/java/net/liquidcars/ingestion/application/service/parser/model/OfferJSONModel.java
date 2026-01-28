package net.liquidcars.ingestion.application.service.parser.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Model specifically for JSON parsing.
 * Aligned with the actual provider JSON format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // <--- CRÍTICO: Evita errores si el JSON trae campos extra
public class OfferJSONModel {

    private String id;
    private String externalId;
    private VehicleTypeJSON vehicleType;
    private String brand;
    private String model;
    private Integer year;
    private BigDecimal price;
    private OfferStatusJSON status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime updatedAt;

    private String source;

    public enum VehicleTypeJSON {
        CAR, TRUCK, MOTORCYCLE, VAN, SUV
    }

    public enum OfferStatusJSON {
        ACTIVE, SOLD, RESERVED, INACTIVE
    }

    public boolean isValid() {
        return externalId != null && !externalId.isBlank()
                && brand != null && !brand.isBlank()
                && model != null && !model.isBlank()
                && year != null && year >= 1900 && year <= LocalDateTime.now().getYear() + 1
                && price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
}