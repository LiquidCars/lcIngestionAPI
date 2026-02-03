package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.application.service.parser.model.OfferXMLModel;
import org.instancio.Instancio;

public class OfferXMLFactory {

    public static OfferXMLModel getOfferXMLModel() {
        return Instancio.create(OfferXMLModel.class);
    }

}
