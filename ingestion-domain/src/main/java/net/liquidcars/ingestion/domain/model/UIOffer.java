package net.liquidcars.ingestion.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
public class UIOffer implements Serializable {
    @Schema(description = "Original offer unique identifier")
    private UUID originalCarOfferId;//Do not add to hashCode implementation
    @Schema(description = "The UICarInstance reference")
    private UIVehicleInstance vehicleInstance;
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
    @Schema(description = "A description of the financed price conditions, advantages, etc.")
    private String financedText;
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
}
