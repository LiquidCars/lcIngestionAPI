package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;

import java.util.List;

public interface IOfferInfraSQLService {

    void processOffer(OfferDto offer);
    void processIngestionReport(IngestionReportDto ingestionReportDto);
    void purgeObsoleteOffers(int daysOld);

    List<IngestionReportDto> getPendingReports();

    void syncPendingReports(List<IngestionReportDto> pendingReports);
}
