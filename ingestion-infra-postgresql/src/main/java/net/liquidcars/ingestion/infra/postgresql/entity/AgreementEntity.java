package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agr_agr_agreements", schema = "public")
public class AgreementEntity {

    @Id
    @Column(name = "agr_co_id")
    private UUID id;

    @Column(name = "agr_ds_name")
    private String name;

    @Column(name = "agr_dt_start")
    private OffsetDateTime startDate;

    @Column(name = "agr_dt_end")
    private OffsetDateTime endDate;

    @Column(name = "agr_bo_enabled")
    private boolean enabled;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vsel_co_idvehicleseller", referencedColumnName = "vsel_co_id")
    private VehicleSellerEntity vehicleSeller;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "agr_ch_channels",
            schema = "public",
            joinColumns = @JoinColumn(name = "agr_co_id"),
            inverseJoinColumns = @JoinColumn(name = "ch_co_id")
    )
    private List<ChannelEntity> channels;
}