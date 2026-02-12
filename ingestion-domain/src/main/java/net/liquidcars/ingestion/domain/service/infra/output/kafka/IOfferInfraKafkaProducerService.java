package net.liquidcars.ingestion.domain.service.infra.output.kafka;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;

public interface IOfferInfraKafkaProducerService {
    void sendOffer(OfferDto offer);
    void sendBatchIngestionJobReport(IngestionBatchReportDto ingestionBatchReportDto);
    void sendIngestionJobReport(IngestionReportDto ingestionReportDto);
}
