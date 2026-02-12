package net.liquidcars.ingestion.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Schema(title="The VehicleOffer DTO object", description="This object represents a commercial offer based con a specific vehicle unit, or VehicleInstance" )
public class OfferDto implements Serializable {

    @Schema(description = "Offer unique identifier")
    private UUID id;//Do not add to hashCode implementation
    @Schema(description = "Type of offer, describing who is the final owner accountable for transaction duties, in case it is not the owner of the inventory (the car seller institution)")
    private CarOfferSellerTypeEnumDto sellerType = CarOfferSellerTypeEnumDto.usedCar_ProfessionalSeller;
    @Schema(description = "The registered user Id of the final owner accountable for transaction duties")
    private UUID privateOwnerRegisteredUserId = null;
    @Schema(description = "The VehicleInstance reference")
    private VehicleInstanceDto vehicleInstance;
    @Schema(description = "The car owner reference")
    private String ownerReference;
    @Schema(description = "The dealer reference")
    private String dealerReference;
    @Schema(description = "The dealer channel reference")
    private String channelReference;
    @Schema(description = "The normal price")
    private MoneyDto price;
    @Schema(description = "The normal price when financing is contracted to buy the car")
    private MoneyDto financedPrice;
    @Schema(description = "If there's a financed price, an approximation to an installment amount, given 48 months and a 20% downpayment")
    private MoneyDto financedInstallmentAprox;
    @Schema(description = "A description of the financed price conditions, advantages, etc.")
    private String financedText;
    @Schema(description = "A reference of the price of the car as new")
    private MoneyDto priceNew;
    @Schema(description = "The price for a professional buyer")
    private MoneyDto professionalPrice;
    @Schema(description = "Indicates if taxes a re deductible")
    private boolean taxDeductible;
    @Schema(description = "Free text used as observations")
    private String obs;
    @Schema(description = "additional notes")
    private String internalNotes;
    @Schema(description = "Collection of additional resources, such as images, documents and any other multimedia content")
    private List<CarOfferResourceDto> resources;
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
    private ParticipantAddressDto pickUpAddress;
    @Schema(description = "A versioning hash code used to detect potential changes of the offer")
    private int hash; //Do not add to hashCode implementation
    @Schema(description = "Update date")
    private long lastUpdated; //default, now. Do not add to hashCode implementation
    @Schema(description = "Update date")
    private UUID jsonCarOfferId;
    @Schema(description = "Participant id")
    private UUID participantId;
    @Schema(description = "Batch job identifier")
    private UUID jobIdentifier;
    @Schema(description = "Batch job status")
    private IngestionBatchStatus batchStatus;
    @Schema(description = "Ingestion report job identifier")
    private UUID ingestionReportId;

    @JsonIgnore
    public UIOffer getUICarOffer(){
        UIOffer ret = new UIOffer();
        ret.setOriginalCarOfferId(getId());
        ret.setVehicleInstance(getVehicleInstance().getUICarInstance());
        ret.setOwnerReference(getVehicleInstance().getChassisNumber());
        ret.setDealerReference(getDealerReference());
        ret.setChannelReference(getChannelReference());
        ret.setPrice(getPrice());
        ret.setFinancedPrice(getFinancedPrice());
        ret.setFinancedText(getFinancedText());
        ret.setProfessionalPrice(getProfessionalPrice());
        ret.setTaxDeductible(isTaxDeductible());
        ret.setObs(getObs());
        ret.setInternalNotes(getInternalNotes());
        ret.setResources(getResources());
        ret.setGuarantee(isGuarantee());
        ret.setGuaranteeMonths(getGuaranteeMonths());
        ret.setGuaranteeText(getGuaranteeText());
        ret.setCertified(isCertified());
        ret.setInstallation(getInstallation());
        ret.setMail(getMail());
        ret.setPickUpAddress(getPickUpAddress());
        return ret;
    }


    @JsonIgnore
    public int getHashCodeCalc(){
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
        if (!(o instanceof OfferDto)) return false;
        OfferDto other = (OfferDto)o;
        return other.hashCode()==this.hashCode();
    }
}
