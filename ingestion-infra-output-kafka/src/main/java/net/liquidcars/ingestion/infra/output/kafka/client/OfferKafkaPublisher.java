package net.liquidcars.ingestion.infra.output.kafka.client;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferKafkaPublisher {

    private final KafkaTemplate<String, OfferMsg> kafkaTemplate;
    private static final String CREATE_OFFER_TOPIC = "liquidcars.ingestion.event.offer.create-action.0";

    public void sendOffer(OfferMsg offer) {
        kafkaTemplate.send(CREATE_OFFER_TOPIC, offer.getId().toString(), offer);
    }
}
