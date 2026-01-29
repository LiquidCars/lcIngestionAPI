package net.liquidcars.ingestion.application.service.parser.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferXMLModel {

    private String id;
    private String externalId;
    private VehicleTypeXML vehicleType;
    private String brand;
    private String model;
    private Integer year;
    private BigDecimal price;
    private OfferStatusXML status;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    private String source;

    public enum VehicleTypeXML {
        CAR, TRUCK, MOTORCYCLE, VAN, SUV
    }

    public enum OfferStatusXML {
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
