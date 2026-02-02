package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.instancio.Instancio;

public class OfferMsgFactory {

    public static OfferMsg getOfferMsg() {
        return Instancio.create(OfferMsg.class);
    }
}

