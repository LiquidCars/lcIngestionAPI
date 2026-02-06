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
@Table(name = "car_eq_equipments")
public class CarInstanceEquipmentEntity {

    @Id
    @Column(name = "eq_co_id")
    private String id;

    @Column(name = "eq_ds_des")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txm_co_id")
    private TextMasterEntity texto;
}
