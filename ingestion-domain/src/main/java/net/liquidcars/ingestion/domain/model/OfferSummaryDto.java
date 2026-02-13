package net.liquidcars.ingestion.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The VehicleOffer DTO object", description="This object represents a summary of an offer. Used to send saved notifications" )
public class OfferSummaryDto implements Serializable {

    @Schema(description = "Offer unique identifier")
    private UUID id;
    @Schema(description = "A versioning hash code used to detect potential changes of the offer")
    private int hash;
    @Schema(description = "The external offer identifiers")
    private ExternalIdInfoDto externalIdInfo;
}
