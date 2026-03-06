package net.liquidcars.ingestion.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TinyLocatorDto {

    private String tinyLocatorId;
    private UUID offerId;
    private UUID inventoryId;
    private UUID agreementId;
    private UUID channelId;
    private UUID vehicleSellerId;
}