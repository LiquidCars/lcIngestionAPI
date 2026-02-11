package net.liquidcars.ingestion.domain.service.application;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

public interface IOfferIngestionProcessService {

    void processOffers(List<OfferDto> offers);
    void processOffersFromUrl(String format, URI url);
    void processOffersStream(String format, InputStream inputStream);

    void syncPendingReports();

    void processIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto);
}
