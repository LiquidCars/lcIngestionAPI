package net.liquidcars.ingestion.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Domain entity representing a vehicle offer.
 * This is a pure domain object with no infrastructure dependencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDto {
    
    private String id = String.valueOf(UUID.randomUUID());
    private String externalId;
    private VehicleTypeDto vehicleType;
    private String brand;
    private String model;
    private Integer year;
    private BigDecimal price;
    private OfferStatusDto status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String source;
    private String jobIdentifier;
    private String batchStatus;
    
    /**
     * Vehicle type enumeration
     */
    public enum VehicleTypeDto {
        CAR,
        TRUCK,
        MOTORCYCLE,
        VAN,
        SUV
    }
    
    /**
     * Offer status enumeration
     */
    public enum OfferStatusDto {
        ACTIVE,
        SOLD,
        RESERVED,
        INACTIVE
    }

    
    /**
     * Business logic: Check if the offer is available for purchase
     */
    public boolean isAvailable() {
        return status == OfferStatusDto.ACTIVE;
    }
    
    /**
     * Business logic: Validate offer data
     */
    public boolean isValid() {
        return externalId != null && !externalId.isBlank()
                && brand != null && !brand.isBlank()
                && model != null && !model.isBlank()
                && year != null && year >= 1900 && year <= LocalDateTime.now().getYear() + 1
                && price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
}
