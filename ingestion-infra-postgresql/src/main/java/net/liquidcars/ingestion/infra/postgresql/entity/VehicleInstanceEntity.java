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
@Table(name = "inv_cari_carinstance")
public class VehicleInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cari_co_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_co_id", nullable = false)
    private VehicleModelEntity vehicleModel;

    @Column(name = "cari_ds_plate")
    private String plate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "col_co_id", nullable = false)
    private ColorEntity color;

    @Column(name = "cari_nm_kmmileage")
    private int mileage;

    @Column(name = "cari_nm_registrationyear")
    private int registrationYear;

    @Column(name = "cari_nm_registrationmonth")
    private int registrationMonth;

    @Column(name = "cari_bo_metallicpaint")
    private boolean isMetallicPaint;

    @Column(name = "cari_ds_chassisnumber")
    private String chassisNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cit_co_id", nullable = false)
    private CarInstanceTypesEntity state;

    @Column(name = "cari_coid_hash")
    private int hash;

    @Column(name = "cari_bo_enabled")
    private boolean enabled;
}
