package net.liquidcars.ingestion.domain.service.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;

import java.util.UUID;

public interface IOfferInfraNoSQLService {

    void processOffer(OfferDto offer);

    void purgeObsoleteOffers(int daysOld);

    long getOffersFromJobId(UUID jobId);
}
