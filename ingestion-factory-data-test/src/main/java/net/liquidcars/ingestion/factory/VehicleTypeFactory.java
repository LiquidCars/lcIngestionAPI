package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.input.rest.model.VehicleType;
import org.instancio.Instancio;

public class VehicleTypeFactory {

    public static VehicleType getVehicleType() {
        return Instancio.create(VehicleType.class);
    }

    public static String getInvalidVehicleType() {
        return "INVALID_" + Instancio.create(String.class);
    }
}
