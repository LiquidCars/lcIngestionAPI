package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.output.kafka.model.VehicleModelMsg;
import org.instancio.Instancio;

public class VehicleModelMsgFactory {

    public static VehicleModelMsg VehicleModelMsg() {
        return Instancio.create(VehicleModelMsg.class);
    }
}
