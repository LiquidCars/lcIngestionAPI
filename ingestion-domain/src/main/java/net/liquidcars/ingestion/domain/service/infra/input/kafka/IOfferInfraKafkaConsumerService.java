package net.liquidcars.ingestion.domain.service.infra.input.kafka;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;

public interface IOfferInfraKafkaConsumerService {
    void processOfferSave(OfferDto offerDto);
    void processIngestionReport(IngestionBatchReportDto ingestionBatchReportDto);
}
