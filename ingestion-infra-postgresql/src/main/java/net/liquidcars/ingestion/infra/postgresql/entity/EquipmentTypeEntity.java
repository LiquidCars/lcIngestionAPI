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
@Table(name = "inv_et_equipmenttypes")
public class EquipmentTypeEntity {

    @Id
    @Column(name = "et_co_id")
    private String id;

    @Column(name = "et_ds_desc")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txm_co_id")
    private TextMasterEntity texto;
}
