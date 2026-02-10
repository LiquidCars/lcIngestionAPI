package net.liquidcars.ingestion.domain.service.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;

import java.util.List;

public interface IOfferInfraNoSQLService {

    void processOffer(OfferDto offer);

    void purgeObsoleteOffers(int daysOld);

    void syncPendingReports(List<IngestionReportDto> pendingReports);
}
