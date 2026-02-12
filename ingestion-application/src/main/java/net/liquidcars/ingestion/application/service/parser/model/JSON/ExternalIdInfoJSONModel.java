package net.liquidcars.ingestion.application.service.parser.model.JSON;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternalIdInfoJSONModel implements Serializable {
    @Schema(description = "The car owner reference")
    private String ownerReference;
    @Schema(description = "The dealer reference")
    private String dealerReference;
    @Schema(description = "The dealer channel reference")
    private String channelReference;
}
