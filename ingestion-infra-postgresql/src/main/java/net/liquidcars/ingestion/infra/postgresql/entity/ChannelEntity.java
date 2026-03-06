package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "op_ch_channels", schema = "public")
public class ChannelEntity {

    @Id
    @Column(name = "ch_co_id", nullable = false)
    private UUID id;

    @Column(name = "chg_co_id", nullable = false)
    private UUID channelGroupId;

    @Column(name = "par_co_id", nullable = false)
    private UUID participantId;

    @Column(name = "ocp_ds_name", nullable = false)
    private String name;

    @Column(name = "ocp_bo_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "jur_co_id", nullable = false)
    private Integer jurisdictionId;

    @Column(name = "ocp_ds_notifications_link")
    private String notificationsLink;

    @Column(name = "ch_dt_creation", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}