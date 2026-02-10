package net.liquidcars.ingestion.infra.output.kafka;

import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.output.kafka.client.OfferKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, OfferMsg> kafkaTemplate;

    @InjectMocks
    private OfferKafkaPublisher publisher;

    private static final String TOPIC = "liquidcars.ingestion.event.offer.create-action.0";

    @Test
    @DisplayName("Should send the offer to the Kafka topic with the ID as the key")
    void sendOffer_ShouldCallKafkaTemplate() {
        OfferMsg msg = OfferMsgFactory.getOfferMsg();
        UUID id = UUID.randomUUID();
        msg.setId(id);

        CompletableFuture<?> future = CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(eq(TOPIC), eq("OFF-999"), eq(msg)))
                .thenReturn((CompletableFuture) future);

        publisher.sendOffer(msg);

        verify(kafkaTemplate, times(1)).send(eq(TOPIC), eq(id.toString()), eq(msg));
    }
}
