package net.liquidcars.ingestion.infra.mongodb.entity;

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
public class AgreementNoSQLEntity {

    private UUID id;
    private UUID vehicleSellerId;
    private List<UUID> channelIds;
}