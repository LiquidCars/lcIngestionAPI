package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OfferNoSQLEntity {

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

    @Field("participant_id")
    private UUID participantId;

    @Field("inventory_id")
    private UUID inventoryId;

}

