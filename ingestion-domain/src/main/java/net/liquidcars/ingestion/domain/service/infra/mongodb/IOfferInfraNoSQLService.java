package net.liquidcars.ingestion.domain.service.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;

public interface IOfferInfraNoSQLService {

    void processOffer(OfferDto offer);
    void processIngestionReport(IngestionReportDto ingestionReportDto);

    void purgeObsoleteOffers(int daysOld);
}
