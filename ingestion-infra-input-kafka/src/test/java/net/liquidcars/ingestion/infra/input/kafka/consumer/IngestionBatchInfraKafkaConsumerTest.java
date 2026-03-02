package net.liquidcars.ingestion.infra.input.kafka.consumer;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.infra.input.kafka.service.mapper.OfferInfraKafkaConsumerMapper;
import net.liquidcars.ingestion.infra.output.kafka.model.BatchIngestionReportMsg;
import org.junit.jupiter.api.BeforeEach;
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
public class IngestionBatchInfraKafkaConsumerTest {

    @InjectMocks
    private IngestionBatchInfraKafkaConsumer consumer;

    @Mock
    private OfferInfraKafkaConsumerMapper mapper;

    @Mock
    private IOfferInfraKafkaConsumerService service;

    private UUID fixedJobId;
    private BatchIngestionReportMsg message;
    private IngestionBatchReportDto reportDto;

    @BeforeEach
    void setUp() {
        // Inicializamos los datos comunes para cada test
        String fixedJobIdString = UUID.randomUUID().toString();

        message = new BatchIngestionReportMsg();
        message.setJobId(fixedJobIdString);

        reportDto = IngestionBatchReportDto.builder()
                .jobId(fixedJobId)
                .build();
    }

    @Test
    @DisplayName("Should process message successfully")
    void consumeOffer_Success() {
        when(mapper.toIngestionReportDto(message)).thenReturn(reportDto);

        assertDoesNotThrow(() -> consumer.consumeBatchReportMsg(message));

        verify(service).processIngestionReport(reportDto);
    }

    @Test
    @DisplayName("Should wrap and rethrow LCIngestionException when service fails")
    void consumeOffer_ServiceFails_ShouldThrowLCIngestionException() {
        // Arrange - Configuramos todo dentro del test para evitar problemas de scope
        String jobId = UUID.randomUUID().toString();
        BatchIngestionReportMsg message = new BatchIngestionReportMsg();
        message.setJobId(jobId);

        IngestionBatchReportDto reportDto = IngestionBatchReportDto.builder()
                .jobId(fixedJobId)
                .build();

        when(mapper.toIngestionReportDto(any())).thenReturn(reportDto);
        doThrow(new RuntimeException("DB Connection Timeout"))
                .when(service).processIngestionReport(any());

        // Act & Assert
        LCIngestionException exception = assertThrows(LCIngestionException.class,
                () -> consumer.consumeBatchReportMsg(message));

        // Verificaciones finales
        assertAll(
                () -> assertEquals(LCTechCauseEnum.DATABASE, exception.getTechCause()),
                () -> assertTrue(exception.getMessage().contains(jobId.toString()), "El mensaje debe contener el Job ID")
        );
    }

    @Test
    @DisplayName("Should wrap and rethrow LCIngestionException when mapper fails")
    void consumeOffer_MapperFails_ShouldThrowLCIngestionException() {
        // Arrange
        when(mapper.toIngestionReportDto(any())).thenThrow(new RuntimeException("Mapping error"));

        // Act & Assert
        assertThrows(LCIngestionException.class, () -> consumer.consumeBatchReportMsg(message));
        verify(service, never()).processIngestionReport(any());
    }
}
