package net.liquidcars.ingestion.infra.output.kafka.service;

import net.liquidcars.ingestion.domain.model.IngestionReportResponseActionDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.OfferSummaryDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.output.kafka.model.*;
import net.liquidcars.ingestion.infra.output.kafka.producer.*;
import net.liquidcars.ingestion.infra.output.kafka.service.mapper.OfferInfraKafkaProducerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferInfraKafkaProducerServiceImplTest {

    @InjectMocks
    private OfferInfraKafkaProducerServiceImpl service;

    @Mock private OfferInfraKafkaProducerMapper mapper;
    @Mock private OfferKafkaPublisher offerKafkaPublisher;
    @Mock private BatchIngestionReportKafkaPublisher batchIngestionReportKafkaPublisher;
    @Mock private IngestionReportKafkaPublisher ingestionReportKafkaPublisher;
    @Mock private OfferSummaryKafkaPublisher offerSummaryKafkaPublisher;
    @Mock private IngestionReportPromoteActionKafkaPublisher promoteActionPublisher;
    @Mock private IngestionReportDeleteActionKafkaPublisher deleteActionPublisher;

    @Test
    @DisplayName("Should map and send offer successfully")
    void sendOffer_Success() {
        // Arrange
        OfferDto dto = OfferDtoFactory.getOfferDto();
        OfferMsg msg = OfferMsgFactory.getOfferMsg();
        when(mapper.toOfferMsg(dto)).thenReturn(msg);

        // Act
        service.sendOffer(dto);

        // Assert
        verify(mapper).toOfferMsg(dto);
        verify(offerKafkaPublisher).sendOffer(msg);
    }

    @Test
    @DisplayName("Should wrap exception when sending offer fails")
    void sendOffer_Failure() {
        // Arrange
        OfferDto dto = OfferDtoFactory.getOfferDto();
        when(mapper.toOfferMsg(any())).thenThrow(new RuntimeException("Mapping error"));

        // Act & Assert
        LCIngestionException ex = assertThrows(LCIngestionException.class, () -> service.sendOffer(dto));
        assertThat(ex.getTechCause()).isEqualTo(LCTechCauseEnum.MESSAGING_BROKER_ERROR);
        verify(offerKafkaPublisher, never()).sendOffer(any());
    }

    @Test
    @DisplayName("Should map and send batch report successfully")
    void sendBatchIngestionJobReport_Success() {
        // Arrange
        IngestionBatchReportDto dto = TestDataFactory.createIngestionBatchReportDto();
        BatchIngestionReportMsg msg = TestDataFactory.createBatchIngestionReportMsg();
        when(mapper.toBatchIngestionReportMsg(dto)).thenReturn(msg);

        // Act
        service.sendBatchIngestionJobReport(dto);

        // Assert
        verify(batchIngestionReportKafkaPublisher).sendBatchIngestionReport(msg);
    }

    @Test
    @DisplayName("Should map and send ingestion report successfully")
    void sendIngestionJobReport_Success() {
        // Arrange
        IngestionReportDto dto = TestDataFactory.createIngestionReport();
        IngestionReportMsg msg = TestDataFactory.createIngestionReportMsg();
        when(mapper.toIngestionReportMsg(dto)).thenReturn(msg);

        // Act
        service.sendIngestionJobReport(dto);

        // Assert
        verify(ingestionReportKafkaPublisher).sendIngestionReport(msg);
    }

    @Test
    @DisplayName("Should map and send saved notification (summary)")
    void sendSavedNotification_Success() {
        // Arrange
        OfferSummaryDto dto = TestDataFactory.createOfferSummaryDto();
        OfferSummaryMsg msg = TestDataFactory.createOfferSummaryMsg();
        when(mapper.toOfferSummaryMsg(dto)).thenReturn(msg);

        // Act
        service.sendSavedNotification(dto);

        // Assert
        verify(offerSummaryKafkaPublisher).sendSummaryOffer(msg);
    }

    @Test
    @DisplayName("Should map and send promote action notification")
    void sendIngestionReportPromoteActionNotification_Success() {
        // Arrange
        IngestionReportResponseActionDto dto = TestDataFactory.createIngestionReportResponseActionDto();
        IngestionReportResponseActionMsg msg = TestDataFactory.createIngestionReportResponseActionMsg();
        when(mapper.toIngestionReportResponseActionMsg(dto)).thenReturn(msg);

        // Act
        service.sendIngestionReportPromoteActionNotification(dto);

        // Assert
        verify(promoteActionPublisher).sendIngestionReportResponseAction(msg);
    }

    @Test
    @DisplayName("Should map and send delete action notification")
    void sendIngestionReportDeleteActionNotification_Success() {
        // Arrange
        IngestionReportResponseActionDto dto = TestDataFactory.createIngestionReportResponseActionDto();
        IngestionReportResponseActionMsg msg = TestDataFactory.createIngestionReportResponseActionMsg();
        when(mapper.toIngestionReportResponseActionMsg(dto)).thenReturn(msg);

        // Act
        service.sendIngestionReportDeleteActionNotification(dto);

        // Assert
        verify(deleteActionPublisher).sendIngestionReportResponseAction(msg);
    }

    @Test
    @DisplayName("Should wrap exception when sending batch report fails")
    void sendBatchIngestionJobReport_Failure() {
        // Arrange
        IngestionBatchReportDto dto = TestDataFactory.createIngestionBatchReportDto();
        when(mapper.toBatchIngestionReportMsg(any())).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        assertThrows(LCIngestionException.class, () -> service.sendBatchIngestionJobReport(dto));
        verify(batchIngestionReportKafkaPublisher, never()).sendBatchIngestionReport(any());
    }

    @Test
    @DisplayName("Should wrap exception when sending ingestion report fails")
    void sendIngestionJobReport_Failure() {
        // Arrange
        IngestionReportDto dto = TestDataFactory.createIngestionReport();
        when(mapper.toIngestionReportMsg(any())).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        assertThrows(LCIngestionException.class, () -> service.sendIngestionJobReport(dto));
        verify(ingestionReportKafkaPublisher, never()).sendIngestionReport(any());
    }

    @Test
    @DisplayName("Should wrap exception when sending saved notification fails")
    void sendSavedNotification_Failure() {
        // Arrange
        OfferSummaryDto dto = TestDataFactory.createOfferSummaryDto();
        when(mapper.toOfferSummaryMsg(any())).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        assertThrows(LCIngestionException.class, () -> service.sendSavedNotification(dto));
        verify(offerSummaryKafkaPublisher, never()).sendSummaryOffer(any());
    }

    @Test
    @DisplayName("Should wrap exception when promote notification fails")
    void sendIngestionReportPromoteActionNotification_Failure() {
        // Arrange
        IngestionReportResponseActionDto dto = TestDataFactory.createIngestionReportResponseActionDto();
        when(mapper.toIngestionReportResponseActionMsg(any())).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        assertThrows(LCIngestionException.class, () -> service.sendIngestionReportPromoteActionNotification(dto));
    }

    @Test
    @DisplayName("Should wrap exception when delete notification fails")
    void sendIngestionReportDeleteActionNotification_Failure() {
        // Arrange
        IngestionReportResponseActionDto dto = TestDataFactory.createIngestionReportResponseActionDto();
        when(mapper.toIngestionReportResponseActionMsg(any())).thenThrow(new RuntimeException("Kafka error"));

        // Act & Assert
        assertThrows(LCIngestionException.class, () -> service.sendIngestionReportDeleteActionNotification(dto));
    }
}
