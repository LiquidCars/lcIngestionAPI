package net.liquidcars.ingestion.domain.model;

import lombok.Data;

@Data
public class InventoryAgreementDto {
    private InventoryAgreementIdDto id;
    private boolean enabled;
}