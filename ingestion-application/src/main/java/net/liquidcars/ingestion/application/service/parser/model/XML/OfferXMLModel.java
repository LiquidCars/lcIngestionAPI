package net.liquidcars.ingestion.application.service.parser.model.XML;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The VehicleOffer XML object", description="This object represents a commercial offer based con a specific vehicle unit, or VehicleInstance" )
public class OfferXMLModel implements Serializable {

    @Schema(description = "Offer unique identifier")
    private UUID id;//Do not add to hashCode implementation
    @Schema(description = "Type of offer, describing who is the final owner accountable for transaction duties, in case it is not the owner of the inventory (the car seller institution)")
    private CarOfferSellerTypeEnumXMLModel sellerType = CarOfferSellerTypeEnumXMLModel.usedCar_ProfessionalSeller;
    @Schema(description = "The registered user Id of the final owner accountable for transaction duties")
    private UUID privateOwnerRegisteredUserId = null;
    @Schema(description = "The VehicleInstance reference")
    private VehicleInstanceXMLModel vehicleInstance;
    @Schema(description = "The car owner reference")
    private String ownerReference;
    @Schema(description = "The dealer reference")
    private String dealerReference;
    @Schema(description = "The dealer channel reference")
    private String channelReference;
    @Schema(description = "The normal price")
    private MoneyXMLModel price;
    @Schema(description = "The normal price when financing is contracted to buy the car")
    private MoneyXMLModel financedPrice;
    @Schema(description = "If there's a financed price, an approximation to an installment amount, given 48 months and a 20% downpayment")
    private MoneyXMLModel financedInstallmentAprox;
    @Schema(description = "A description of the financed price conditions, advantages, etc.")
    private String financedText;
    @Schema(description = "A reference of the price of the car as new")
    private MoneyXMLModel priceNew;
    @Schema(description = "The price for a professional buyer")
    private MoneyXMLModel professionalPrice;
    @Schema(description = "Indicates if taxes a re deductible")
    private boolean taxDeductible;
    @Schema(description = "Free text used as observations")
    private String obs;
    @Schema(description = "additional notes")
    private String internalNotes;
    @Schema(description = "Collection of additional resources, such as images, documents and any other multimedia content")
    private List<CarOfferResourceXMLModel> resources;
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
    private ParticipantAddressXMLModel pickUpAddress;
    @Schema(description = "A versionning hash code used to detect potential changes of the offer")
    private int hash; //Do not add to hashCode implementation
    @Schema(description = "Update date")
    private long lastUpdated = DateHelperXMLModel.now().toEpochSecond(ZoneOffset.UTC); //default, now. Do not add to hashCode implementation
    @Schema(description = "Update date")
    private UUID xmlCarOfferId;

    public boolean isValid() {
        return id != null
                && sellerType != null
                && price != null && price.getAmount() != null && price.getAmount().compareTo(BigDecimal.ZERO) > 0
                && resources != null && !resources.isEmpty()
                && isSellerContextValid();
    }

    /**
     * Valida que la relación entre el tipo de vendedor y su ID sea coherente
     */
    private boolean isSellerContextValid() {
        if (CarOfferSellerTypeEnumXMLModel.usedCar_PrivateSeller.equals(sellerType)) {
            return privateOwnerRegisteredUserId != null;
        }
        // Si es profesional, se asume que dealerReference o ownerReference deberían existir
        return true;
    }

}
