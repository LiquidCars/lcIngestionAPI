package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelNoSQLEntity {
    @Field("brand")
    private String brand;

    @Field("model")
    private String model;

    @Field("version")
    private String version;

    @Field("euro_tax_code")
    private String euroTaxCode;

    @Field("body_type")
    private KeyValueNoSQLEntity bodyType;

    @Field("num_doors")
    private Integer numDoors;

    @Field("cv")
    private Integer cv;

    @Field("num_cylinders")
    private Integer numCylinders;

    @Field("displacement")
    private Integer displacement;

    @Field("urban_consumption")
    private Double urbanConsumption;

    @Field("road_consumption")
    private Double roadConsumption;

    @Field("avg_consumption")
    private Double avgConsumption;

    @Field("num_gears")
    private Integer numGears;

    @Field("kg_weight")
    private Integer kgWeight;

    @Field("change_type")
    private KeyValueNoSQLEntity changeType;

    @Field("fuel_type")
    private KeyValueNoSQLEntity fuelType;

    @Field("num_seats")
    private Integer numSeats;

    @Field("drivetrain_type")
    private KeyValueNoSQLEntity drivetrainType;

    @Field("environmental_badge")
    private KeyValueNoSQLEntity environmentalBadge;

    @Field("cm_width")
    private Integer cmWidth;

    @Field("cm_length")
    private Integer cmLength;

    @Field("cm_height")
    private Integer cmHeight;

    @Field("litres_trunk")
    private Integer litresTrunk;

    @Field("litres_tank")
    private Integer litresTank;

    @Field("max_speed")
    private Integer maxSpeed;

    @Field("max_emissions")
    private Integer maxEmissions;

    @Field("acceleration")
    private Double acceleration;
}
