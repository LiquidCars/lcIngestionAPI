package net.liquidcars.ingestion.infra.postgresql.entity.report;

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
@Table(name = "sm_sm_statemachine")
public class StateMachineEntity {

    @Id
    @Column(name = "sm_co_id", nullable = false, updatable = false)
    private String id;

    @Column(name = "sm_ds_desc")
    private String description;

    @Column(name = "sm_bo_end_on_first_final", nullable = false)
    private boolean endOnFirstFinal;

    @Column(name = "sm_bo_enabled", nullable = false)
    private boolean enabled;
}
