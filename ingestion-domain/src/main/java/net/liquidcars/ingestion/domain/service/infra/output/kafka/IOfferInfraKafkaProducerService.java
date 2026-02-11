package net.liquidcars.ingestion.domain.service.infra.output.kafka;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;

public interface IOfferInfraKafkaProducerService {
    void sendOffer(OfferDto offer);
    void sendJobReport(IngestionBatchReportDto ingestionBatchReportDto);
}
