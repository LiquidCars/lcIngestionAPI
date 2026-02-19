package net.liquidcars.ingestion.infra.output.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferKafkaPublisher {

    private final KafkaTemplate<String, OfferMsg> kafkaTemplate;
    private static final String CREATE_OFFER_TOPIC = "liquidcars.ingestion.event.offer.create-action.0";

    public void sendOffer(OfferMsg offer) {
        try {
            log.info("Sending kafka topic {}: {}", CREATE_OFFER_TOPIC, offer);
            kafkaTemplate.send(CREATE_OFFER_TOPIC, offer.getId().toString(), offer).get();
        } catch (Exception e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Error sending offer with id: "+offer.getId())
                    .cause(e)
                    .build();
        }
    }
}
