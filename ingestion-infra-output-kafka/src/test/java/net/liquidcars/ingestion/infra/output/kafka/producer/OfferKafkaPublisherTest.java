package net.liquidcars.ingestion.infra.output.kafka.producer;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferKafkaPublisherTest {

    @InjectMocks
    private OfferKafkaPublisher publisher;

    @Mock
    private KafkaTemplate<String, OfferMsg> kafkaTemplate;

    private static final String TOPIC = "liquidcars.ingestion.event.offer.create-action.0";

    @Test
    @DisplayName("Should send offer successfully to Kafka")
    void sendOffer_Success() {
        // Arrange: Generamos un OfferMsg completo aleatorio con Instancio
        OfferMsg offer = OfferMsgFactory.getOfferMsg();

        CompletableFuture<SendResult<String, OfferMsg>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq(TOPIC), eq(offer.getId().toString()), eq(offer)))
                .thenReturn(future);

        // Act
        publisher.sendOffer(offer);

        // Assert
        verify(kafkaTemplate, times(1)).send(TOPIC, offer.getId().toString(), offer);
    }

    @Test
    @DisplayName("Should throw LCIngestionException and wrap ExecutionException when Kafka fails")
    void sendOffer_Failure_ShouldThrowCustomException() {
        // Arrange
        OfferMsg offer = OfferMsgFactory.getOfferMsg();
        String errorMessage = "Kafka timeout";

        // Simulamos fallo asíncrono
        CompletableFuture<SendResult<String, OfferMsg>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException(errorMessage));

        when(kafkaTemplate.send(anyString(), anyString(), any(OfferMsg.class)))
                .thenReturn(future);

        // Act & Assert
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> publisher.sendOffer(offer));

        // Verificamos el contrato de error
        assertThat(exception.getTechCause()).isEqualTo(LCTechCauseEnum.MESSAGING_BROKER_ERROR);
        assertThat(exception.getMessage()).contains(offer.getId().toString());

        // Verificamos la cadena de excepciones debida al .get()
        assertThat(exception.getCause()).isInstanceOf(ExecutionException.class);
        assertThat(exception.getCause().getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getCause().getMessage()).isEqualTo(errorMessage);
    }
}
