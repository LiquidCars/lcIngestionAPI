package net.liquidcars.ingestion.application.service.parser.model.JSON;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Model specifically for JSON parsing.
 * Aligned with the actual provider JSON format.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // <--- CRÍTICO: Evita errores si el JSON trae campos extra
@Schema(title="The VehicleOffer JSON object", description="This object represents a commercial offer based con a specific vehicle unit, or VehicleInstance" )
public class OfferJSONModel implements Serializable {

    @Schema(description = "Offer unique identifier")
    private UUID id;//Do not add to hashCode implementation
    @Schema(description = "Type of offer, describing who is the final owner accountable for transaction duties, in case it is not the owner of the inventory (the car seller institution)")
    private CarOfferSellerTypeEnumJSONModel sellerType = CarOfferSellerTypeEnumJSONModel.usedCar_ProfessionalSeller;
    @Schema(description = "The registered user Id of the final owner accountable for transaction duties")
    private UUID privateOwnerRegisteredUserId = null;
    @Schema(description = "The VehicleInstance reference")
    private VehicleInstanceJSONModel vehicleInstance;
    @Schema(description = "The external offer identifiers")
    private ExternalIdInfoJSONModel externalIdInfo;
    @Schema(description = "The normal price")
    private MoneyJSONModel price;
    @Schema(description = "The normal price when financing is contracted to buy the car")
    private MoneyJSONModel financedPrice;
    @Schema(description = "If there's a financed price, an approximation to an installment amount, given 48 months and a 20% downpayment")
    private MoneyJSONModel financedInstallmentAprox;
    @Schema(description = "A description of the financed price conditions, advantages, etc.")
    private String financedText;
    @Schema(description = "A reference of the price of the car as new")
    private MoneyJSONModel priceNew;
    @Schema(description = "The price for a professional buyer")
    private MoneyJSONModel professionalPrice;
    @Schema(description = "Indicates if taxes a re deductible")
    private boolean taxDeductible;
    @Schema(description = "Free text used as observations")
    private String obs;
    @Schema(description = "additional notes")
    private String internalNotes;
    @Schema(description = "Collection of additional resources, such as images, documents and any other multimedia content")
    private List<CarOfferResourceJSONModel> resources;
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
    private ParticipantAddressJSONModel pickUpAddress;
    @Schema(description = "A versionning hash code used to detect potential changes of the offer")
    private int hash; //Do not add to hashCode implementation
    @Schema(description = "Update date")
    private long lastUpdated; //default, now. Do not add to hashCode implementation
    @Schema(description = "Update date")
    private UUID jsonCarOfferId;

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
        if (CarOfferSellerTypeEnumJSONModel.usedCar_PrivateSeller.equals(sellerType)) {
            return privateOwnerRegisteredUserId != null;
        }
        // Si es profesional, se asume que dealerReference o ownerReference deberían existir
        return true;
    }
}