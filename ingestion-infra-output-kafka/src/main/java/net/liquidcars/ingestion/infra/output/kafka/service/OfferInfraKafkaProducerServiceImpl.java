package net.liquidcars.ingestion.infra.output.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.infra.output.kafka.client.OfferKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import net.liquidcars.ingestion.infra.output.kafka.service.mapper.OfferInfraKafkaProducerMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraKafkaProducerServiceImpl implements IOfferInfraKafkaProducerService {

    private final OfferInfraKafkaProducerMapper offerInfraKafkaProducerMapper;
    private final OfferKafkaPublisher offerKafkaPublisher;

    @Override
    public void sendOffer(OfferDto offer) {
        try {
            OfferMsg offerMsg = offerInfraKafkaProducerMapper.toOfferMsg(offer);
            offerKafkaPublisher.sendOffer(offerMsg);
        } catch (Exception e) {
            log.error("Failed to dispatch offer to Kafka topic. OfferId: {}", offer.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Infrastructure failure: Kafka publisher is unavailable")
                    .cause(e)
                    .build();
        }
    }
}
