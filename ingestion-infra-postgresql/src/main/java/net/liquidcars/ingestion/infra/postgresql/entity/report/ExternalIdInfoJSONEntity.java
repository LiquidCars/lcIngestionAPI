package net.liquidcars.ingestion.infra.postgresql.entity.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalIdInfoJSONEntity {
    private String ownerReference;
    private String dealerReference;
    private String channelReference;
}