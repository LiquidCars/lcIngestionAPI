package net.liquidcars.ingestion.infra.output.kafka.producer;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.BatchIngestionReportMsg;
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
public class BatchIngestionReportKafkaPublisherTest {

    @InjectMocks
    private BatchIngestionReportKafkaPublisher publisher;

    @Mock
    private KafkaTemplate<String, BatchIngestionReportMsg> kafkaTemplate;

    private static final String TOPIC = "liquidcars.ingestion.event.batchreport.executed-action.0";

    @Test
    @DisplayName("Should send batch report successfully to Kafka")
    void sendBatchIngestionReport_Success() {
        // Arrange
        BatchIngestionReportMsg report = TestDataFactory.createBatchIngestionReportMsg();

        CompletableFuture<SendResult<String, BatchIngestionReportMsg>> future =
                CompletableFuture.completedFuture(mock(SendResult.class));

        // Verificamos que se usa el JobId como Partition Key
        when(kafkaTemplate.send(eq(TOPIC), eq(report.getJobId()), eq(report)))
                .thenReturn(future);

        // Act
        publisher.sendBatchIngestionReport(report);

        // Assert
        verify(kafkaTemplate, times(1)).send(TOPIC, report.getJobId(), report);
    }

    @Test
    @DisplayName("Should wrap ExecutionException into LCIngestionException when Kafka delivery fails")
    void sendBatchIngestionReport_Failure_ShouldThrowCustomException() {
        // Arrange
        BatchIngestionReportMsg report = TestDataFactory.createBatchIngestionReportMsg();
        String internalError = "Broker connection timeout";

        CompletableFuture<SendResult<String, BatchIngestionReportMsg>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException(internalError));

        when(kafkaTemplate.send(anyString(), any(), any(BatchIngestionReportMsg.class)))
                .thenReturn(future);

        // Act & Assert
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> publisher.sendBatchIngestionReport(report));

        // Verificaciones del envoltorio de error
        assertThat(exception.getTechCause()).isEqualTo(LCTechCauseEnum.MESSAGING_BROKER_ERROR);
        assertThat(exception.getMessage()).contains(report.getJobId().toString());

        // Verificación de la jerarquía de causas (Future.get() -> ExecutionException -> RuntimeException)
        assertThat(exception.getCause()).isInstanceOf(ExecutionException.class);
        assertThat(exception.getCause().getCause()).isInstanceOf(RuntimeException.class);
        assertThat(exception.getCause().getCause().getMessage()).isEqualTo(internalError);
    }
}
