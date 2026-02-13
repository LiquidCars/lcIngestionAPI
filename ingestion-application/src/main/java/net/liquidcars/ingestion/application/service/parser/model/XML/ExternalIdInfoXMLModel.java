package net.liquidcars.ingestion.application.service.parser.model.XML;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternalIdInfoXMLModel implements Serializable {
    @Schema(description = "The car owner reference")
    private String ownerReference;
    @Schema(description = "The dealer reference")
    private String dealerReference;
    @Schema(description = "The dealer channel reference")
    private String channelReference;
}
