package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@Table(name = "agr_nin_namedinventories", schema = "public")
public class InventoryAgreementEntity {
    @EmbeddedId
    private InventoryAgreementId id;

    @Column(name = "nin_bo_enabled")
    private boolean enabled;
}