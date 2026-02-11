package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(CarInstanceEquipmentId.class)
@Table(name = "inv_cot_carinstanceequipments")
public class CarInstanceEquipmentEntity {

    @Id
    @Column(name = "cot_co_id")
    private Integer id;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cari_co_id")
    private VehicleInstanceEntity vehicleInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eq_co_id")
    private EquipmentsEntity equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ec_co_id")
    private EquipmentCategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "et_co_id")
    private EquipmentTypeEntity type;

    @Column(name = "cot_ds_desc")
    private String description;

    @Column(name = "cot_ds_code")
    private String code;

    @Column(name = "cot_nm_price")
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cur_co_id", nullable = false)
    private CurrencyEntity currency;
}
