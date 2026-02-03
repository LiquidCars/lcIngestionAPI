package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.instancio.Instancio;

public class OfferRequestFactory {

    public static OfferRequest getOfferRequest() {
        return Instancio.create(OfferRequest.class);
    }
}
