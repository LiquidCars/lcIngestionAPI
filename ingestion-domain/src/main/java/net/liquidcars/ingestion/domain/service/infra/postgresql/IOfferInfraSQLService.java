package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.OfferDto;

public interface IOfferInfraSQLService {

    void processOffer(OfferDto offer);

}
