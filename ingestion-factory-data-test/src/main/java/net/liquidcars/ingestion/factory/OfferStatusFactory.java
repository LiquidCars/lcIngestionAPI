package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.input.rest.model.OfferStatus;
import org.instancio.Instancio;

public class OfferStatusFactory {

    public static OfferStatus getOfferStatus() {
        return Instancio.create(OfferStatus.class);
    }

    public static String getInvalidOfferStatus() {
        return "INVALID_STATUS_" + Instancio.create(String.class);
    }
}
