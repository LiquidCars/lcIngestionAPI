package net.liquidcars.ingestion.infra.output.kafka.producer;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferSummaryMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferSummaryKafkaPublisherTest {

    @InjectMocks
    private OfferSummaryKafkaPublisher publisher;

    @Mock
    private KafkaTemplate<String, OfferSummaryMsg> kafkaTemplate;

    @Test
    @DisplayName("Should send message successfully to Kafka")
    void sendSummaryOffer_Success() {
        // Arrange
        OfferSummaryMsg summary = TestDataFactory.createOfferSummaryMsg();
        String expectedTopic = "liquidcars.ingestion.event.offer.saved-action.0";

        // Simulamos que Kafka responde correctamente
        CompletableFuture<SendResult<String, OfferSummaryMsg>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq(expectedTopic), eq(summary.getId().toString()), eq(summary)))
                .thenReturn(future);

        // Act
        publisher.sendSummaryOffer(summary);

        // Assert
        verify(kafkaTemplate, times(1))
                .send(expectedTopic, summary.getId().toString(), summary);
    }

    @Test
    @DisplayName("Should throw LCIngestionException when Kafka fails")
    void sendSummaryOffer_Failure_ShouldThrowCustomException() {
        // Arrange
        OfferSummaryMsg summary = TestDataFactory.createOfferSummaryMsg();

        // Simulamos un error en la red/Kafka
        CompletableFuture<SendResult<String, OfferSummaryMsg>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka connection lost"));

        when(kafkaTemplate.send(anyString(), anyString(), any(OfferSummaryMsg.class)))
                .thenReturn(future);

        // Act & Assert
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> publisher.sendSummaryOffer(summary));

        assertThat(exception.getTechCause()).isEqualTo(LCTechCauseEnum.MESSAGING_BROKER_ERROR);
        assertThat(exception.getMessage()).contains(summary.getId().toString());
        assertThat(exception.getCause()).isNotNull();
    }
}
