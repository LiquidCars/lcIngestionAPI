package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import org.instancio.Instancio;

public class OfferEntityFactory {

    public static OfferEntity getOfferEntity() {
        return Instancio.create(OfferEntity.class);
    }

}
