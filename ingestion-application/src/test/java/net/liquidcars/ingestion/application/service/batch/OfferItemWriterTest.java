package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferItemWriterTest {

    @Mock
    private IOfferInfraKafkaProducerService kafkaProducer;

    @InjectMocks
    private OfferItemWriter offerItemWriter;

    private StepExecution stepExecution;

    // IDs de prueba
    private final UUID ingestionId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID participantId = UUID.randomUUID();
    private final UUID inventoryId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Configuramos los JobParameters que espera el Writer
        JobParameters params = new JobParametersBuilder()
                .addString("ingestionId", ingestionId.toString())
                .addString("ingestionReportId", reportId.toString())
                .addString("requesterParticipantId", participantId.toString())
                .addString("inventoryId", inventoryId.toString())
                .toJobParameters();

        JobExecution jobExecution = new JobExecution(1L, params);
        stepExecution = new StepExecution("offerStep", jobExecution);

        // Inicializamos el listener manualmente
        offerItemWriter.beforeStep(stepExecution);
    }

    @Test
    @DisplayName("Debe setear los IDs del Job en el OfferDto y enviarlo a Kafka")
    void shouldEnrichOfferWithJobParametersAndSendToKafka() {
        // GIVEN
        OfferDto offer = TestDataFactory.createOfferDto();
        Chunk<OfferDto> chunk = new Chunk<>(List.of(offer));

        // WHEN
        offerItemWriter.write(chunk);

        // THEN
        ArgumentCaptor<OfferDto> captor = ArgumentCaptor.forClass(OfferDto.class);
        verify(kafkaProducer, times(1)).sendOffer(captor.capture());

        OfferDto enrichedOffer = captor.getValue();
        assertThat(enrichedOffer.getJobIdentifier()).isEqualTo(ingestionId);
        assertThat(enrichedOffer.getIngestionReportId()).isEqualTo(reportId);
        assertThat(enrichedOffer.getParticipantId()).isEqualTo(participantId);
        assertThat(enrichedOffer.getInventoryId()).isEqualTo(inventoryId);
    }

    @Test
    @DisplayName("Debe manejar múltiples ofertas en un mismo Chunk")
    void shouldProcessAllOffersInChunk() {
        // GIVEN
        List<OfferDto> offers = List.of(TestDataFactory.createOfferDto(), TestDataFactory.createOfferDto(), TestDataFactory.createOfferDto());
        Chunk<OfferDto> chunk = new Chunk<>(offers);

        // WHEN
        offerItemWriter.write(chunk);

        // THEN
        verify(kafkaProducer, times(3)).sendOffer(any(OfferDto.class));
    }

    @Test
    @DisplayName("Debe setear nulos si los JobParameters no están presentes")
    void shouldHandleMissingJobParametersGracefully() {
        // GIVEN: Re-configuramos con parámetros vacíos
        JobExecution jobExecution = new JobExecution(2L, new JobParameters());
        StepExecution emptyStepExecution = new StepExecution("emptyStep", jobExecution);
        offerItemWriter.beforeStep(emptyStepExecution);

        OfferDto offer = TestDataFactory.createOfferDto();
        Chunk<OfferDto> chunk = new Chunk<>(List.of(offer));

        // WHEN
        offerItemWriter.write(chunk);

        // THEN
        assertThat(offer.getJobIdentifier()).isNull();
        assertThat(offer.getParticipantId()).isNull();
        verify(kafkaProducer).sendOffer(offer);
    }

    @Test
    @DisplayName("Debe fallar o loguear si stepExecution no ha sido inicializado")
    void shouldHandleNullStepExecution() {
        // GIVEN: Forzamos el estado nulo (no llamamos a beforeStep)
        OfferItemWriter uninitializedWriter = new OfferItemWriter(kafkaProducer);
        OfferDto offer = TestDataFactory.createOfferDto();
        Chunk<OfferDto> chunk = new Chunk<>(List.of(offer));

        // WHEN
        uninitializedWriter.write(chunk);

        // THEN
        assertThat(offer.getJobIdentifier()).isNull();
        verify(kafkaProducer).sendOffer(offer);
    }
}
