package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inv_car_cars")
public class VehicleModelEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "car_co_id")
    private Long id;

    @Column(name = "car_co_brand")
    private String brand;

    @Column(name = "car_co_model")
    private String model;

    @Column(name = "car_co_version")
    private String version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bdt_co_id")
    private BodyTypesEntity bodyType;

    @Column(name = "car_nm_doors")
    private Integer numDoors;

    @Column(name = "car_nm_cv")
    private Integer cv;

    @Column(name = "car_nm_cylinders")
    private Integer numCylinders;

    @Column(name = "car_nm_displacement")
    private Integer displacement;

    @Column(name = "car_nm_urbanconsumtion", columnDefinition = "numeric")
    private Double urbanConsumption;

    @Column(name = "car_nm_roadconsumption", columnDefinition = "numeric")
    private Double roadConsumption;

    @Column(name = "car_nm_avgconsumption", columnDefinition = "numeric")
    private Double avgConsumption;

    @Column(name = "car_nm_changegears")
    private Integer numGears;

    @Column(name = "car_nm_kgweight")
    private Integer kgWeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cht_co_id")
    private ChangeTypesEntity changeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fut_co_id")
    private FuelTypesEntity fuelType;

    @Column(name = "car_nm_seats")
    private Integer numSeats;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dtt_co_id")
    private DriveTrainTypeEntity drivetrainType;

    @Column(name = "car_co_eurotaxcode")
    private String euroTaxCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "el_co_id")
    private EnvironmentalBadgeEntity environmentalBadge;

    @Column(name = "car_nm_cm_width")
    private Integer cmWidth;

    @Column(name = "car_nm_cm_length")
    private Integer cmLength;

    @Column(name = "car_nm_cm_height")
    private Integer cmHeight;

    @Column(name = "car_nm_lt_trunk")
    private Integer litresTrunk;

    @Column(name = "car_nm_lt_tank")
    private Integer litresTank;

    @Column(name = "car_nm_kmh_max_speed")
    private Integer maxSpeed;

    @Column(name = "car_nm_max_emissions")
    private Integer maxEmissions;

    @Column(name = "car_nm_acceleration", columnDefinition = "numeric")
    private Double acceleration;

    @Column(name = "car_coid_hash")
    private long hash;

    @Column(name = "car_bo_enabled")
    private boolean enabled;
}
