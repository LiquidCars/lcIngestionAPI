package net.liquidcars.ingestion.application.service.parser.model.XML;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(title="The CarInstance XML object", description="This object represents the specific car unit itself" )
public class VehicleInstanceXMLModel implements Serializable {
    @Schema(description = "internal ID")
    private long id;    //Do not add to hashCode implementation
    @Schema(description = "Override of de VehicleModel description")
    private VehicleModelXMLModel vehicleModel;
    @Schema(description = "The actual car plate", example = "1234XJV")
    private String plate;
    @Schema(description = "The car color", example = "Red")
    private KeyValueXMLModel color;
    @Schema(description = "Aproximate mileage, in kilometers", example = "123.456")
    private int mileage;
    @Schema(description = "The year of official registration", example = "2013")
    private int registrationYear;
    @Schema(description = "The month of official registration", example = "6")
    private int registrationMonth;
    @Schema(description = "It indicates if the paint color has a metallic finish", example = "True")
    private boolean isMetallicPaint;
    @Schema(description = "The actual chassis (frame) number", example = "SV30-0169266")
    private String chassisNumber;
    @Schema(description = "The list of existing off-series equipement included in this specific car")
    private List<CarInstanceEquipmentXMLModel> equipments;
    @Schema(description = "The state type of this car")
    private KeyValueXMLModel state;

    public int hashCode(){
        //Init numbers should be primes, and different for each HashCodeBuilder in different classes
        return Math.abs(new HashCodeBuilder(17,31)
                .append(vehicleModel !=null ? vehicleModel.hashCode() : 0).append(plate).append(color).append(mileage).append(registrationYear)
                .append(registrationMonth).append(isMetallicPaint).append(chassisNumber).append(state)
                .toHashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof VehicleInstanceXMLModel)) return false;
        VehicleInstanceXMLModel other = (VehicleInstanceXMLModel)o;
        return other.hashCode()==this.hashCode();
    }
}
