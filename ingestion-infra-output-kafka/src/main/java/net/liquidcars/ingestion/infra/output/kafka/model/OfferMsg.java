package net.liquidcars.ingestion.infra.output.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.*;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Domain entity representing a vehicle offer.
 * This is a pure domain object with no infrastructure dependencies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The VehicleOffer Message object", description="This object represents a commercial offer based con a specific vehicle unit, or VehicleInstance" )
public class OfferMsg implements Serializable {

    @Schema(description = "Offer unique identifier")
    private UUID id;//Do not add to hashCode implementation
    @Schema(description = "Type of offer, describing who is the final owner accountable for transaction duties, in case it is not the owner of the inventory (the car seller institution)")
    private CarOfferSellerTypeEnumMsg sellerType = CarOfferSellerTypeEnumMsg.usedCar_ProfessionalSeller;
    @Schema(description = "The registered user Id of the final owner accountable for transaction duties")
    private UUID privateOwnerRegisteredUserId = null;
    @Schema(description = "The VehicleInstance reference")
    private VehicleInstanceMsg vehicleInstance;
    @Schema(description = "The external offer identifiers")
    private ExternalIdInfoMsg externalIdInfo;
    @Schema(description = "The normal price")
    private MoneyMsg price;
    @Schema(description = "The normal price when financing is contracted to buy the car")
    private MoneyMsg financedPrice;
    @Schema(description = "If there's a financed price, an approximation to an installment amount, given 48 months and a 20% downpayment")
    private MoneyMsg financedInstallmentAprox;
    @Schema(description = "A description of the financed price conditions, advantages, etc.")
    private String financedText;
    @Schema(description = "A reference of the price of the car as new")
    private MoneyMsg priceNew;
    @Schema(description = "The price for a professional buyer")
    private MoneyMsg professionalPrice;
    @Schema(description = "Indicates if taxes a re deductible")
    private boolean taxDeductible;
    @Schema(description = "Free text used as observations")
    private String obs;
    @Schema(description = "additional notes")
    private String internalNotes;
    @Schema(description = "Collection of additional resources, such as images, documents and any other multimedia content")
    private List<CarOfferResourceMsg> resources;
    @Schema(description = "Indicates if the car is currently under some kind of guarantee")
    private boolean guarantee;
    @Schema(description = "Number of months the guarantee is still available")
    private int guaranteeMonths;
    @Schema(description = "A Free text describing the guarantee conditions")
    private String guaranteeText;
    @Schema(description = "A flag indicating if the car has been certified by the seller or any other third party")
    private boolean certified;
    @Schema(description = "A free text describing the installation")
    private String installation;
    @Schema(description = "A contact email")
    private String mail;
    @Schema(description = "Pickup location, if described")
    private ParticipantAddressMsg pickUpAddress;
    @Schema(description = "A versionning hash code used to detect potential changes of the offer")
    private int hash; //Do not add to hashCode implementation
    @Schema(description = "Update date")
    private long lastUpdated; //default, now. Do not add to hashCode implementation
    @Schema(description = "Update date")
    private UUID jsonCarOfferId;
    @Schema(description = "Participant id")
    private UUID participantId;
    @Schema(description = "Batch job identifier")
    private UUID jobIdentifier;
    @Schema(description = "Ingestion report job identifier")
    private UUID ingestionReportId;
    @Schema(description = "Inventory of offers")
    private UUID inventoryId;


    @JsonIgnore
    public int getHashCodeCalc(){
        String ownerReference = null;
        String dealerReference = null;
        String channelReference = null;
        if(externalIdInfo!=null){
            ownerReference = externalIdInfo.getOwnerReference();
            dealerReference = externalIdInfo.getDealerReference();
            channelReference = externalIdInfo.getChannelReference();
        }
        //Init numbers should be primes, and different for each HashCodeBuilder in different classes
        return Math.abs(new HashCodeBuilder(61,73)
                .append(vehicleInstance !=null ? vehicleInstance.hashCode() : 0).append(ownerReference)
                .append(dealerReference).append(channelReference).append(price).append(financedPrice).append(financedInstallmentAprox)
                .append(financedText).append(priceNew).append(professionalPrice).append(taxDeductible).append(obs).append(internalNotes)
                .append(guarantee).append(guaranteeMonths).append(guaranteeText)
                .append(certified).append(installation).append(mail).append(pickUpAddress)
                .toHashCode());
    }
    @Override
    public int hashCode(){
        //Note: the hash is calculated in ingestion time and stored in offer.hash and in the DB;
        //  by default, it's equal to the function hashCode(), but it can be changed by some transformations
        //  and / or inheritances of the CarOffer object, that may have the effect of generate a different hashCode()
        //  So, we use the "hash" value, if any, as it's the original one, except if it's not there, in which case we use the function
        //  to calculate it (it will not exist only when storing it the first time or if we create a CarOffer instance from scratch)

        if (this.getHash()!=0) return this.getHash();
        return getHashCodeCalc();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof OfferMsg)) return false;
        OfferMsg other = (OfferMsg)o;
        return other.hashCode()==this.hashCode();
    }
}
