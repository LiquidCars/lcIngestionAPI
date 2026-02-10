package net.liquidcars.ingestion.application.service.parser.model.JSON;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(title="The CarInstanceEquipment JSON object", description="This object represents an optional equipment included in a give car" )
public class CarInstanceEquipmentJSONModel implements Serializable {
    @JsonIgnore
    private long vehicleInstanceId;
    @Schema(description = "internal ID")
    private int id;
    @Schema(description = "The equipment label")
    private KeyValueJSONModel equipment;
    @Schema(description = "The equipment category")
    private KeyValueJSONModel category;
    @Schema(description = "The equipment type")
    private KeyValueJSONModel type;
    @Schema(description = "A text description of the equipment")
    private String description;
    @Schema(description = "The equipment code")
    private String code;
    @Schema(description = "The equipment price indicator")
    private MoneyJSONModel price;
}
