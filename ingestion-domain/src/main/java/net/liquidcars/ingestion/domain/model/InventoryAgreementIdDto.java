package net.liquidcars.ingestion.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class InventoryAgreementIdDto implements Serializable {
    private UUID agreementId;
    private UUID inventoryId;
}
