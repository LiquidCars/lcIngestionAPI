package net.liquidcars.ingestion.domain.service.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;

import java.util.UUID;

public interface IOfferInfraNoSQLService {

    void processOffer(OfferDto offer);

    void purgeObsoleteOffers(int daysOld);

    long countOffersFromJobId(UUID jobId);

    long countOffersFromReportId(UUID ingestionReportId);
}
