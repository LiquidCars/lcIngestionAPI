package net.liquidcars.ingestion.domain.service.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;

public interface IOfferInfraNoSQLService {

    OfferDto save(OfferDto offer);
}
