package net.liquidcars.ingestion.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class UIVehicleModel implements Serializable {
    @Schema(description = "internal ID")
    private long id; //Do not add to hashCode implementation
    @Schema(description = "OEM manufacturer brand", example = "BMW")
    private String brand;
    @Schema(description = "OEM manufacturer model name", example = "530D")
    private String model;
    @Schema(description = "OEM manufacturer model version", example = "F10")
    private String version;
    @Schema(description = "EuroTax code, if any")
    private String euroTaxCode;
    @Schema(description = "The body type of the car")
    private KeyValueDto bodyType;
    @Schema(description = "The number of doors")
    private Integer numDoors;
    @Schema(description = "Horse power")
    private Integer cv;
    @Schema(description = "Number of cylinders")
    private Integer numCylinders;
    @Schema(description = "Engine displacement in cubic centimeters")
    private Integer displacement;
    @Schema(description = "ICE engine consumption in urban conditions")
    private Double urbanConsumption;
    @Schema(description = "ICE engine consumption in raod trip conditions")
    private Double roadConsumption;
    @Schema(description = "ICE engine mixed/average consumption")
    private Double avgConsumption;
    @Schema(description = "Gear number")
    private Integer numGears;
    @Schema(description = "Weight in kilograms")
    private Integer kgWeight;
    @Schema(description = "Change type")
    private KeyValueDto changeType;
    @Schema(description = "Fuel type")
    private KeyValueDto fuelType;
    @Schema(description = "Number of seats")
    private Integer numSeats;
    @Schema(description = "Drivetrain type")
    private KeyValueDto drivetrainType;
    @Schema(description = "Environmental badge")
    private KeyValueDto environmentalBadge;
    @Schema(description = "Width in centimeters")
    private Integer cmWidth;
    @Schema(description = "Length in centimeters")
    private Integer cmLength;
    @Schema(description = "Height in centimeters")
    private Integer cmHeight;
    @Schema(description = "Trunk volume in litres")
    private Integer litresTrunk;
    @Schema(description = "Gas tank volume in litres")
    private Integer litresTank;
    @Schema(description = "Maximum speed")
    private Integer maxSpeed;
    @Schema(description = "Maximum emissions")
    private Integer maxEmissions;
    @Schema(description = "Maximum acceleration")
    private Double acceleration;
}
