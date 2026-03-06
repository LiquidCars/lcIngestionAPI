package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "op_vsel_vehiclesellers", schema = "public")
public class VehicleSellerEntity {
    @Id
    @Column(name = "vsel_co_id")
    private UUID id;

    @Column(name = "vsel_ds_name")
    private String name;

    @Column(name = "vsel_bo_enabled")
    private boolean enabled;
}