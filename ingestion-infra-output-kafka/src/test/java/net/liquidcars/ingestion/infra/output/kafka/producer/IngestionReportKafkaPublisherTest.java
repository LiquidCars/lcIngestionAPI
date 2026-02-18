package net.liquidcars.ingestion.infra.output.kafka.producer;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportMsg;
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
public class IngestionReportKafkaPublisherTest {

    @InjectMocks
    private IngestionReportKafkaPublisher publisher;

    @Mock
    private KafkaTemplate<String, IngestionReportMsg> kafkaTemplate;

    private static final String TOPIC = "liquidcars.ingestion.event.report.updated-action.0";

    @Test
    @DisplayName("Should send ingestion report successfully to Kafka")
    void sendIngestionReport_Success() {
        // Arrange
        IngestionReportMsg report = TestDataFactory.createIngestionReportMsg();

        CompletableFuture<SendResult<String, IngestionReportMsg>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        when(kafkaTemplate.send(eq(TOPIC), eq(report.getId()), eq(report)))
                .thenReturn(future);

        // Act
        publisher.sendIngestionReport(report);

        // Assert
        verify(kafkaTemplate, times(1)).send(TOPIC, report.getId(), report);
    }

    @Test
    @DisplayName("Should wrap ExecutionException into LCIngestionException when Kafka fails")
    void sendIngestionReport_Failure_ShouldThrowCustomException() {
        // Arrange
        IngestionReportMsg report = TestDataFactory.createIngestionReportMsg();
        String internalError = "Kafka connection timeout";

        // Simulamos el fallo asíncrono
        CompletableFuture<SendResult<String, IngestionReportMsg>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException(internalError));

        when(kafkaTemplate.send(anyString(), any(), any(IngestionReportMsg.class)))
                .thenReturn(future);

        // Act & Assert
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> publisher.sendIngestionReport(report));

        // Verificamos el contrato de error de tu dominio
        assertThat(exception.getTechCause()).isEqualTo(LCTechCauseEnum.MESSAGING_BROKER_ERROR);
        assertThat(exception.getMessage()).contains(report.getId());

        // Verificamos que la causa raíz esté envuelta en una ExecutionException (por el .get())
        assertThat(exception.getCause()).isInstanceOf(ExecutionException.class);
        assertThat(exception.getCause().getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getCause().getMessage()).isEqualTo(internalError);
    }
}
