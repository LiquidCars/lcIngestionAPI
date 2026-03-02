package net.liquidcars.ingestion.infra.input.kafka.consumer;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportActionMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionReportDeleteInfraKafkaConsumerTest {

    @InjectMocks
    private IngestionReportDeleteInfraKafkaConsumer consumer;

    @Mock
    private IOfferInfraKafkaConsumerService offerInfraKafkaConsumerService;

    @Test
    @DisplayName("Should call delete action successfully when message is valid")
    void consumeIngestionReportActionDelete_Success() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        IngestionReportActionMsg message = new IngestionReportActionMsg();
        message.setIngestionReportId(reportId);

        // Act
        assertDoesNotThrow(() -> consumer.consumeIngestionReportActionDelete(message));

        // Assert
        verify(offerInfraKafkaConsumerService, times(1))
                .processIngestionReportDeleteAction(reportId);
    }

    @Test
    @DisplayName("Should throw LCIngestionException when service fails during delete")
    void consumeIngestionReportActionDelete_ServiceFails_ShouldThrowLCIngestionException() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        IngestionReportActionMsg message = new IngestionReportActionMsg();
        message.setIngestionReportId(reportId);

        doThrow(new RuntimeException("Database error on delete"))
                .when(offerInfraKafkaConsumerService)
                .processIngestionReportDeleteAction(any(UUID.class));

        // Act & Assert
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> consumer.consumeIngestionReportActionDelete(message));

        assertAll(
                () -> assertEquals(LCTechCauseEnum.DATABASE, exception.getTechCause()),
                () -> assertTrue(exception.getMessage().contains(reportId.toString())),
                () -> assertTrue(exception.getMessage().contains("for delete offers")),
                () -> assertEquals("Database error on delete", exception.getCause().getMessage())
        );
    }

    @Test
    @DisplayName("Should throw LCIngestionException if service fails due to null ID")
    void consumeIngestionReportActionDelete_NullId_ShouldThrowLCIngestionException() {
        IngestionReportActionMsg message = new IngestionReportActionMsg();
        message.setIngestionReportId(null);

        doThrow(new IllegalArgumentException("ID cannot be null"))
                .when(offerInfraKafkaConsumerService)
                .processIngestionReportDeleteAction(null);

        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> consumer.consumeIngestionReportActionDelete(message));

        assertEquals(LCTechCauseEnum.DATABASE, exception.getTechCause());
        verify(offerInfraKafkaConsumerService, times(1)).processIngestionReportDeleteAction(null);
    }
}
