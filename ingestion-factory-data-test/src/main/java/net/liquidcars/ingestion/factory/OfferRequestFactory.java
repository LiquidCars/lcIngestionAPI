package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class OfferRequestFactory {

    public static OfferRequest getOfferRequest() {
        return Instancio.create(OfferRequest.class);
    }

    public static OfferRequest createWithResources(int resourceCount) {
        return Instancio.of(OfferRequest.class)
                .generate(field(OfferRequest::getResources),
                        gen -> gen.collection().size(resourceCount))
                .create();
    }

    public static OfferRequest createForComparison() {
        // Usamos Instancio para que el objeto tenga datos y no sea un objeto vacío
        return Instancio.create(OfferRequest.class);
    }
}
