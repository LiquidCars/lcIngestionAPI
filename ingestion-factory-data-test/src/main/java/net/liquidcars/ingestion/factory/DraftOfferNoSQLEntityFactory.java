package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import org.instancio.Instancio;

public class DraftOfferNoSQLEntityFactory {

    public static DraftOfferNoSQLEntity getDraftOfferNoSQLEntity() {
        return Instancio.create(DraftOfferNoSQLEntity.class);
    }

}
