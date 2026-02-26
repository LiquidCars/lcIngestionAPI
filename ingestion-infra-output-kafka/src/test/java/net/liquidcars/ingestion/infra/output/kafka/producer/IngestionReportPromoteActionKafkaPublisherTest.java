package net.liquidcars.ingestion.infra.output.kafka.producer;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportResponseActionMsg;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionReportPromoteActionKafkaPublisherTest {

    @InjectMocks
    private IngestionReportPromoteActionKafkaPublisher publisher;

    @Mock
    private KafkaTemplate<String, IngestionReportResponseActionMsg> kafkaTemplate;

    private static final String TOPIC = "liquidcars.ingestion.event.report.promote-action-result.0";

    @Test
    @DisplayName("Should send delete action response successfully to Kafka")
    void sendIngestionReportResponseAction_Success() {
        // Arrange
        IngestionReportResponseActionMsg message = TestDataFactory.createIngestionReportResponseActionMsg();

        // Simulamos respuesta exitosa de Kafka
        CompletableFuture<SendResult<String, IngestionReportResponseActionMsg>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq(TOPIC), eq(message.getIngestionReportId().toString()), eq(message)))
                .thenReturn(future);

        // Act
        publisher.sendIngestionReportResponseAction(message);

        // Assert
        verify(kafkaTemplate, times(1)).send(TOPIC, message.getIngestionReportId().toString(), message);
    }

    @Test
    @DisplayName("Should throw LCIngestionException when Kafka delivery fails")
    void sendIngestionReportResponseAction_Failure_ShouldThrowCustomException() {
        // Arrange
        IngestionReportResponseActionMsg message = TestDataFactory.createIngestionReportResponseActionMsg();

        // Simulamos que el futuro falla
        CompletableFuture<SendResult<String, IngestionReportResponseActionMsg>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka Broker unavailable"));

        when(kafkaTemplate.send(anyString(), anyString(), any(IngestionReportResponseActionMsg.class)))
                .thenReturn(future);

        // Act & Assert
        // 1. Verificamos que lanza la excepción personalizada de tu dominio
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> publisher.sendIngestionReportResponseAction(message));

        // 2. Verificamos los detalles de tu excepción personalizada
        assertThat(exception.getTechCause()).isEqualTo(LCTechCauseEnum.MESSAGING_BROKER_ERROR);
        assertThat(exception.getMessage()).contains(message.getIngestionReportId().toString());

        // 3. ¡IMPORTANTE! La causa de LCIngestionException será una ExecutionException
        // debido al .get() del CompletableFuture
        assertThat(exception.getCause()).isInstanceOf(java.util.concurrent.ExecutionException.class);
        assertThat(exception.getCause().getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getCause().getMessage()).isEqualTo("Kafka Broker unavailable");
    }
}
