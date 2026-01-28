package net.liquidcars.ingestion.domain.service.infra.output.kafka;

import net.liquidcars.ingestion.domain.model.OfferDto;

public interface IOfferInfraKafkaProducerService {
    void sendOffer(OfferDto offer);
}
