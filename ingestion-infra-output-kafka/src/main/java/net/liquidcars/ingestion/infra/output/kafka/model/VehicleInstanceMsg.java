package net.liquidcars.ingestion.infra.output.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.liquidcars.ingestion.domain.model.*;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(title="The CarInstance Message object", description="This object represents the specific car unit itself" )
public class VehicleInstanceMsg implements Serializable {
    @Schema(description = "internal ID")
    private long id;    //Do not add to hashCode implementation
    @Schema(description = "Override of de VehicleModel description")
    private VehicleModelMsg vehicleModel;
    @Schema(description = "The actual car plate", example = "1234XJV")
    private String plate;
    @Schema(description = "The car color", example = "Red")
    private KeyValueMsg color;
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
    private List<CarInstanceEquipmentMsg> equipments;
    @Schema(description = "The state type of this car")
    private KeyValueMsg state;

    @JsonIgnore
    public LocalDateTime getRegistrationApproxDate(boolean approxWithMileage) {
        if (this.getRegistrationYear()>1900) {
            int month = this.getRegistrationMonth()>0 ? this.getRegistrationMonth() : 1;
            return DateHelperMsg.fromStringLDT(this.getRegistrationYear() + "-" + (month < 10 ? "0" : "") + month + "-01", DateHelperMsg.DEFAULT_DATE_ONLY_FORMAT_STR);
        }
        else {
            if (approxWithMileage) {
                //Assume 20K Kms/year
                int years = 1 + this.getMileage() / 20000;
                return DateHelperMsg.now().minusYears(years);
            }
            else {
                return null;
            }
        }
    }


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
        if (!(o instanceof VehicleInstanceMsg)) return false;
        VehicleInstanceMsg other = (VehicleInstanceMsg)o;
        return other.hashCode()==this.hashCode();
    }
}
