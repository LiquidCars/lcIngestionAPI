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
@Table(name = "car_dtt_drivetraintypes")
public class DriveTrainTypeEntity {

    @Id
    @Column(name = "dtt_co_id")
    private String id;

    @Column(name = "dtt_ds_des")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txm_co_id")
    private TextMasterEntity texto;
}
