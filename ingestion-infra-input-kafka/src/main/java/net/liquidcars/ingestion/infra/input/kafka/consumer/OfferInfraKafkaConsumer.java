package net.liquidcars.ingestion.infra.input.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.infra.input.kafka.service.mapper.OfferInfraKafkaConsumerMapper;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferInfraKafkaConsumer {

    private final OfferInfraKafkaConsumerMapper offerInfraKafkaConsumerMapper;
    private final IOfferInfraKafkaConsumerService offerInfraKafkaConsumerService;

    @KafkaListener(
            topics = "liquidcars.ingestion.event.offer.create-action.0",
            groupId = "liquidcars-ingestion-group"
    )
    public void consumeOffer(OfferMsg message) {
        log.info("Received offer to process. Offer ID: {}", message.getId());
        try {
            OfferDto offerDto = offerInfraKafkaConsumerMapper.toOfferDto(message);
            offerInfraKafkaConsumerService.processOfferSave(offerDto);
        } catch (Exception e) {
            log.error("Critical error processing offer: {}. Triggering Kafka retry...", message.getId(), e);
            // We wrap and rethrow the exception.
            // By letting it propagate, Kafka's ErrorHandler will catch it.
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to process consumed offer: " + message.getId())
                    .cause(e)
                    .build();
        }
    }
}