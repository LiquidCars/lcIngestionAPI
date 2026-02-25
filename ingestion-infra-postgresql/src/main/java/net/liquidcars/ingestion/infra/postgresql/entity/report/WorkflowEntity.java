package net.liquidcars.ingestion.infra.postgresql.entity.report;

import jakarta.persistence.*;
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
@Table(name = "wrk_wrk_workflows")
public class WorkflowEntity {

    @Id
    @Column(name = "wrk_co_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "wrk_dt_creation", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sm_co_id", nullable = false)
    private StateMachineEntity stateMachine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "sm_co_id", referencedColumnName = "sm_co_id", insertable = false, updatable = false),
            @JoinColumn(name = "smv_nm_id", referencedColumnName = "smv_nm_id", insertable = false, updatable = false)
    })
    private StateMachineVersionEntity stateMachineVersion;

    @Column(name = "wrk_external_id", nullable = false)
    private UUID externalId;

    @Column(name = "wrk_bo_finished", nullable = false)
    private boolean finished;

    @Column(name = "wrk_dt_end")
    private OffsetDateTime endDate;

    @Column(name = "wrk_bo_end_on_first_final", nullable = false)
    private boolean endOnFirstFinal;

    @Column(name = "wrk_bo_enabled", nullable = false)
    private boolean enabled;
}
