package net.liquidcars.ingestion.domain.service.infra.input.kafka;

import net.liquidcars.ingestion.domain.model.OfferDto;

public interface IOfferInfraKafkaConsumerService {
    void processOfferSave(OfferDto offerDto);
}
