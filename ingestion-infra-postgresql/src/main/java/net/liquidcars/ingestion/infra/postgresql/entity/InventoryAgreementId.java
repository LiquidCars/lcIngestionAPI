package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
public class InventoryAgreementId implements Serializable {
    @Column(name = "agr_co_id")
    private UUID agreementId;
    @Column(name = "nin_co_id")
    private UUID inventoryId;
}
