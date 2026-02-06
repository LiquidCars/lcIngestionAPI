package net.liquidcars.ingestion.domain.service.infra.input.kafka;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;

public interface IOfferInfraKafkaConsumerService {
    void processOfferSave(OfferDto offerDto);
    void processIngestionReport(IngestionReportDto ingestionReportDto);
}
