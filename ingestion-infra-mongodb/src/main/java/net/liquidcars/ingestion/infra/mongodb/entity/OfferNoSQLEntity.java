package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.OfferDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "offers_raw")
@CompoundIndex(
        name = "idx_offers_external_id",
        def = "{ 'external_id': 1 }",
        unique = true
)
public class OfferNoSQLEntity {

    @Id
    private String id;

    @Field("external_id")
    private String externalId;

    @Field("vehicle_type")
    private VehicleType vehicleType;

    private String brand;
    private String model;
    private Integer year;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal price;

    private OfferStatus status;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    private String source;

    /**
     * Vehicle type enumeration
     */
    public enum VehicleType {
        CAR,
        TRUCK,
        MOTORCYCLE,
        VAN,
        SUV
    }

    /**
     * Offer status enumeration
     */
    public enum OfferStatus {
        ACTIVE,
        SOLD,
        RESERVED,
        INACTIVE
    }

}

