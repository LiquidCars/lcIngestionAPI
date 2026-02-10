package net.liquidcars.ingestion.infra.output.kafka.client;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferKafkaPublisher {

    private final KafkaTemplate<String, OfferMsg> kafkaTemplate;
    private static final String CREATE_OFFER_TOPIC = "liquidcars.ingestion.event.offer.create-action.0";

    public void sendOffer(OfferMsg offer) {
        try {
            kafkaTemplate.send(CREATE_OFFER_TOPIC, offer.getId(), offer).get();
        } catch (Exception e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Error sending offer with id: "+offer.getId())
                    .cause(e)
                    .build();
        }
    }
}
