package net.liquidcars.ingestion.infra.input.kafka.service;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.OfferSummaryDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferInfraKafkaConsumerServiceImplTest {

    @InjectMocks
    private OfferInfraKafkaConsumerServiceImpl service;

    @Mock
    private IOfferInfraNoSQLService offerInfraNoSQLService;

    @Mock
    private IOfferInfraSQLService offerInfraSQLService;

    @Mock
    private IOfferIngestionProcessService ingestionProcessService;

    @Mock
    private IOfferInfraKafkaProducerService kafkaProducerService;

    @Test
    @DisplayName("Should save to NoSQL and notify via Kafka when offer is processed")
    void processOfferSave_Success() {
        // Arrange
        OfferDto offer = OfferDtoFactory.getOfferDto();
        ArgumentCaptor<OfferSummaryDto> summaryCaptor = ArgumentCaptor.forClass(OfferSummaryDto.class);

        // Act
        service.processOfferSave(offer);

        // Assert
        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(kafkaProducerService, times(1)).sendSavedNotification(summaryCaptor.capture());

        OfferSummaryDto captured = summaryCaptor.getValue();
        assertEquals(offer.getId(), captured.getId());
        assertEquals(offer.getHash(), captured.getHash());
    }

    @Test
    @DisplayName("Should NOT send Kafka notification if NoSQL persistence fails")
    void processOfferSave_FailureNoSQL_ShouldNotNotify() {
        // Arrange
        OfferDto offer = OfferDtoFactory.getOfferDto();
        doThrow(new RuntimeException("Database Error")).when(offerInfraNoSQLService).processOffer(any());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> service.processOfferSave(offer));
        verify(kafkaProducerService, never()).sendSavedNotification(any());
    }

    @Test
    @DisplayName("Should delegate batch report to ingestion process service")
    void processIngestionReport_ShouldDelegate() {
        // Arrange
        IngestionBatchReportDto report = IngestionBatchReportDto.builder()
                .jobId(UUID.randomUUID())
                .readCount(100)
                .writeCount(95)
                .build();

        // Act
        service.processIngestionReport(report);

        // Assert
        verify(ingestionProcessService, times(1)).processIngestionBatchReport(report);
    }

    @Test
    @DisplayName("Should promote draft offers with promote flag as true")
    void processIngestionReportPromoteAction_ShouldDelegateWithTrueFlag() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        // Act
        service.processIngestionReportPromoteAction(jobId);

        // Assert
        verify(ingestionProcessService, times(1)).promoteDraftOffersToVehicleOffers(jobId, true);
    }

    @Test
    @DisplayName("Should delete draft offers with delete flag as true")
    void processIngestionReportDeleteAction_ShouldDelegateWithTrueFlag() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        // Act
        service.processIngestionReportDeleteAction(jobId);

        // Assert
        verify(ingestionProcessService, times(1)).deleteDraftOffersByIngestionReportId(jobId, true);
    }

}
