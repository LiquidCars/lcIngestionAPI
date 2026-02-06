package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "offers")
public class OfferEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type")
    private VehicleTypeDto vehicleType;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "year")
    private Integer year;

    @Column(name = "price")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OfferStatusDto status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "source")
    private String source;

    @Column(name = "jobIdentifier")
    private String jobIdentifier;

    @Column(name = "batchStatus")
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

}
