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
public class IngestionReportPromoteInfraKafkaConsumerTest {

    @InjectMocks
    private IngestionReportPromoteInfraKafkaConsumer consumer;

    @Mock
    private IOfferInfraKafkaConsumerService offerInfraKafkaConsumerService;

    @Test
    @DisplayName("Should call promote action successfully when message is valid")
    void consumePromoteOffers_Success() {
        UUID reportId = UUID.randomUUID();
        IngestionReportActionMsg message = new IngestionReportActionMsg();
        message.setIngestionReportId(reportId);

        assertDoesNotThrow(() -> consumer.consumeIngestionReportActionPromote(message));

        verify(offerInfraKafkaConsumerService, times(1))
                .processIngestionReportPromoteAction(reportId);
    }

    @Test
    @DisplayName("Should throw LCIngestionException when service fails")
    void consumePromoteOffers_ServiceFails_ShouldThrowLCIngestionException() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        IngestionReportActionMsg message = new IngestionReportActionMsg();
        message.setIngestionReportId(reportId);

        doThrow(new RuntimeException("Service Error"))
                .when(offerInfraKafkaConsumerService)
                .processIngestionReportPromoteAction(any(UUID.class));

        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> consumer.consumeIngestionReportActionPromote(message));

        assertAll(
                () -> assertEquals(LCTechCauseEnum.DATABASE, exception.getTechCause()),
                () -> assertTrue(exception.getMessage().contains(reportId.toString())),
                () -> assertTrue(exception.getMessage().contains("promote offers"))
        );
    }

    @Test
    @DisplayName("Should throw LCIngestionException if service fails due to null ID")
    void consumePromoteOffers_NullId_ShouldThrowLCIngestionException() {
        IngestionReportActionMsg message = new IngestionReportActionMsg();
        message.setIngestionReportId(null);

        doThrow(new IllegalArgumentException("ID cannot be null"))
                .when(offerInfraKafkaConsumerService)
                .processIngestionReportPromoteAction(null);

        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> consumer.consumeIngestionReportActionPromote(message));

        assertEquals(LCTechCauseEnum.DATABASE, exception.getTechCause());
        assertTrue(exception.getMessage().contains("Failed to process"));
        verify(offerInfraKafkaConsumerService, times(1)).processIngestionReportPromoteAction(null);
    }
}
