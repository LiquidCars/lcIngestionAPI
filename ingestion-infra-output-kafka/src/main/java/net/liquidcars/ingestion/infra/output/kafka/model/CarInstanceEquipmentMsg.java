package net.liquidcars.ingestion.infra.output.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The CarInstanceEquipment Message object", description="This object represents an optional equipment included in a give car" )
public class CarInstanceEquipmentMsg implements Serializable {
    @JsonIgnore
    private long vehicleInstanceId;
    @Schema(description = "internal ID")
    private int id;
    @Schema(description = "The equipment label")
    private KeyValueMsg equipment;
    @Schema(description = "The equipment category")
    private KeyValueMsg category;
    @Schema(description = "The equipment type")
    private KeyValueMsg type;
    @Schema(description = "A text description of the equipment")
    private String description;
    @Schema(description = "The equipment code")
    private String code;
    @Schema(description = "The equipment price indicator")
    private MoneyMsg price;
}
