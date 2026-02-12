package net.liquidcars.ingestion.infra.mongodb.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vehicleoffers_ingestion_draft")
@CompoundIndex(
        name = "idx_offers_composite",
        def = "{ 'owner_reference': 1, 'dealer_reference': 1, 'channel_reference': 1 }"
)
public class OfferNoSQLEntity {

    @Id
    private String id;

    @Field("seller_type")
    private CarOfferSellerTypeEnumNoSQLEntity sellerType;

    @Field("private_owner_user_id")
    private String privateOwnerRegisteredUserId;

    @Field("vehicle_instance")
    private VehicleInstanceNoSQLEntity vehicleInstance;

    @Field("owner_reference")
    @Indexed
    private String ownerReference;

    @Field("dealer_reference")
    @Indexed
    private String dealerReference;

    @Field("channel_reference")
    @Indexed
    private String channelReference;

    @Field("price")
    private MoneyNoSQLEntity price;

    @Field("financed_price")
    private MoneyNoSQLEntity financedPrice;

    @Field("financed_installment_aprox")
    private MoneyNoSQLEntity financedInstallmentAprox;

    @Field("financed_text")
    private String financedText;

    @Field("price_new")
    private MoneyNoSQLEntity priceNew;

    @Field("professional_price")
    private MoneyNoSQLEntity professionalPrice;

    @Field("tax_deductible")
    private boolean taxDeductible;

    @Field("obs")
    private String obs;

    @Field("internal_notes")
    private String internalNotes;

    @Field("resources")
    private List<CarOfferResourceNoSQLEntity> resources;

    @Field("guarantee")
    private boolean guarantee;

    @Field("guarantee_months")
    private int guaranteeMonths;

    @Field("guarantee_text")
    private String guaranteeText;

    @Field("certified")
    private boolean certified;

    @Field("installation")
    private String installation;

    @Field("mail")
    private String mail;

    @Field("pickup_address")
    private ParticipantAddressNoSQLEntity pickUpAddress;

    @Field("hash")
    @Indexed
    private int hash; //Do not add to hashCode implementation

    @Field("last_updated")
    private long lastUpdated; //default, now. Do not add to hashCode implementation

    @Field("json_car_offer_id")
    private String jsonCarOfferId;

    @Field("created_at")
    private Instant createdAt;

    @Field("updated_at")
    private Instant updatedAt;

    @Field("job_identifier")
    private String jobIdentifier;

    @Field("batch_status")
    private String batchStatus;

    @Field("ingestion_report_id")
    private UUID ingestionReportId;

}

