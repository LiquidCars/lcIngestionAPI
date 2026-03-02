package net.liquidcars.ingestion.infra.output.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferSummaryMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferSummaryKafkaPublisher {

    private final KafkaTemplate<String, OfferSummaryMsg> kafkaTemplate;
    private static final String CREATE_OFFER_TOPIC = "liquidcars.ingestion.event.offer.saved-action.0";

    public void sendSummaryOffer(OfferSummaryMsg offerSummary) {
        try {
            log.info("Sending kafka topic {}: {}", CREATE_OFFER_TOPIC, offerSummary);
            kafkaTemplate.send(CREATE_OFFER_TOPIC, offerSummary.getId().toString(), offerSummary).get();
        } catch (Exception e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Error saving offer with id: " + offerSummary.getId())
                    .cause(e)
                    .build();
        }
    }
}
