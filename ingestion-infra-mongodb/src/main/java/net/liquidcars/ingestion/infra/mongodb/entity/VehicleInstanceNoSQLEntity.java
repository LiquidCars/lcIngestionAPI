package net.liquidcars.ingestion.infra.mongodb.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInstanceNoSQLEntity {

    @Field("vehicle_model")
    private VehicleModelNoSQLEntity vehicleModel;

    @Field("plate")
    private String plate;

    @Field("color")
    private KeyValueNoSQLEntity color;

    @Field("mileage")
    private int mileage;

    @Field("registration_year")
    private int registrationYear;

    @Field("registration_month")
    private int registrationMonth;

    @Field("is_metallic_paint")
    private boolean isMetallicPaint;

    @Field("chassis_number")
    private String chassisNumber;

    @Field("equipments")
    private List<CarInstanceEquipmentNoSQLEntity> equipments;

    @Field("state")
    private KeyValueNoSQLEntity state;

}
