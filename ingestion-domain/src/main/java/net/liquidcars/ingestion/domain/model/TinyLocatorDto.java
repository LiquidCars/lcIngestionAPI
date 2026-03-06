package net.liquidcars.ingestion.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
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
    private UUID vehicleShellerId;
}