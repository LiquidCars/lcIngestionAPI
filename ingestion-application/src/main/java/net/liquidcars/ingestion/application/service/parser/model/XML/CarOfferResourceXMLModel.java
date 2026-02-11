package net.liquidcars.ingestion.application.service.parser.model.XML;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The CarOfferResource XML object", description="This object represents a multimedia resource associated with car offers" )
public class CarOfferResourceXMLModel implements Serializable {
    @JsonIgnore
    private UUID offerId;

    @Schema(description = "Id")
    private int id;
    @Schema(description = "A type describing the nature of the resource: image or video, as URL or raw resource")
    private KeyValueXMLModel<String,String> type;
    @Schema(description = "A resource locator, URL when the type is URL based, or the resource content when it's raw.")
    private String resource;
    private byte[] compressedResource;
}
