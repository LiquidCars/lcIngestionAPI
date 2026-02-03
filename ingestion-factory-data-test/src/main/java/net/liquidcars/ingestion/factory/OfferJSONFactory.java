package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.application.service.parser.model.OfferJSONModel;
import org.instancio.Instancio;

public class OfferJSONFactory {

    public static OfferJSONModel getOfferJSONModel() {
        return Instancio.create(OfferJSONModel.class);
    }

}
