package net.liquidcars.ingestion.infra.output.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The CarOfferResource Message object", description="This object represents a multimedia resource associated with car offers" )
public class CarOfferResourceMsg implements Serializable {
    @JsonIgnore
    private UUID offerId;

    @Schema(description = "Id")
    private int id;
    @Schema(description = "A type describing the nature of the resource: image or video, as URL or raw resource")
    private KeyValueMsg<String,String> type;
    @Schema(description = "A resource locator, URL when the type is URL based, or the resource content when it's raw.")
    private String resource;
    private byte[] compressedResource;

}
