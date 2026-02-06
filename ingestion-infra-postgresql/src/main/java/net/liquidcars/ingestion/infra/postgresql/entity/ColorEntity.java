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
@Table(name = "car_col_colors")
public class ColorEntity {

    @Id
    @Column(name = "col_co_id")
    private String id;

    @Column(name = "col_ds_des")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "txm_co_id")
    private TextMasterEntity texto;
}
