package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import org.instancio.Instancio;

public class OfferNoSQLEntityFactory {

    public static OfferNoSQLEntity getOfferNoSQLEntity() {
        return Instancio.create(OfferNoSQLEntity.class);
    }

}
