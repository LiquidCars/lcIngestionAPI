package net.liquidcars.ingestion.infra.postgresql.entity.report;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StateMachineVersionEntity.StateMachineVersionId.class)
@Table(name = "wrk_smv_state_machine_versions")
public class StateMachineVersionEntity {

    @Id
    @Column(name = "sm_co_id")
    private String stateMachineId;

    @Id
    @Column(name = "smv_nm_id")
    private Integer versionId;

    @Column(name = "smv_json", nullable = false, columnDefinition = "text")
    private String json;

    @Column(name = "smv_nm_hash", nullable = false)
    private Long hash;

    @Column(name = "smv_dt_created", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sm_co_id", insertable = false, updatable = false)
    private StateMachineEntity stateMachine;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StateMachineVersionId implements Serializable {
        private String stateMachineId;
        private Integer versionId;
    }
}
