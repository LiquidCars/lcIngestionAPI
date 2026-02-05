package net.liquidcars.ingestion.application.service;

import net.liquidcars.ingestion.application.service.batch.IngestionSkipListener;
import net.liquidcars.ingestion.application.service.batch.JobCompletionNotificationListener;
import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OfferIngestionProcessServiceImplTest {

    @InjectMocks
    private OfferIngestionProcessServiceImpl service;

    @Mock
    private List<IOfferParserService> parsers;

    @Mock
    private IOfferParserService mockParser;

    @Mock
    private IOfferInfraKafkaProducerService offerInfraKafkaProducerService;

    @Mock
    private OfferItemWriter offerItemWriter;

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job offerIngestionJob;

    @Mock
    private OfferStreamItemReader offerReader;

    @Mock
    private IngestionSkipListener ingestionSkipListener;

    @Mock
    private JobCompletionNotificationListener jobCompletionListener;

    @Captor
    private ArgumentCaptor<OfferDto> offerCaptor;

    @BeforeEach
    void setUp() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        // Todos los tests usarán mockParser como parser válido
        lenient().when(parsers.stream()).thenAnswer(i -> Stream.of(mockParser));

        // Mockear los métodos que corren hilos virtuales
        lenient().doAnswer(invocation -> null)
                .when(offerReader).start(any(IOfferParserService.class), any(InputStream.class));

        lenient().doAnswer(invocation -> null)
                .when(jobLauncher).run(any(Job.class), any());
    }

    @Test
    void processOffers_ShouldSendEachOfferToKafka() {
        OfferDto offer1 = OfferDtoFactory.getOfferDto();
        OfferDto offer2 = OfferDtoFactory.getOfferDto();
        List<OfferDto> offers = List.of(offer1, offer2);

        service.processOffers(offers);

        verify(offerInfraKafkaProducerService, times(2)).sendOffer(offerCaptor.capture());
        assertEquals(2, offerCaptor.getAllValues().size());
    }

    @Test
    void processOffers_ShouldThrowException_WhenOffersListIsEmpty() {
        List<OfferDto> emptyList = List.of();

        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffers(emptyList)
        );

        assertEquals("The offers list is empty or null", ex.getMessage());
    }

    @Test
    void processOffersFromUrl_ShouldStartJob_WhenParserSupportsFormat() throws Exception {
        String format = "json";
        InputStream mockInputStream = new ByteArrayInputStream("[{}]".getBytes());

        when(mockParser.supports(format)).thenReturn(true);
        when(parsers.stream()).thenReturn(Stream.of(mockParser));
        when(jobLauncher.run(any(Job.class), any()))
                .thenReturn(mock(org.springframework.batch.core.JobExecution.class));

        service.processOffersStream(format, mockInputStream);

        // Espera hasta que offerReader.start sea llamado
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() ->
                verify(offerReader, atLeastOnce()).start(any(IOfferParserService.class), any(InputStream.class))
        );

        verify(jobLauncher, atLeastOnce()).run(any(Job.class), any());
    }

    @Test
    void processOffersStream_ShouldStartJob_WhenParserSupportsFormat() throws Exception {
        String format = "json";
        InputStream is = new ByteArrayInputStream("[{}]".getBytes());

        when(mockParser.supports(format)).thenReturn(true);

        assertDoesNotThrow(() -> service.processOffersStream(format, is));

        Thread.sleep(100); // Esperar al hilo virtual

        verify(offerReader, atLeastOnce()).start(any(IOfferParserService.class), any(InputStream.class));
        verify(jobLauncher, atLeastOnce()).run(any(Job.class), any());
    }

    @Test
    void processOffersStream_ShouldThrowException_WhenFormatNotSupported() {
        String format = "unsupported";
        InputStream inputStream = InputStream.nullInputStream();
        when(parsers.stream()).thenReturn(Stream.empty());

        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffersStream(format, inputStream)
        );

        assertTrue(ex.getMessage().contains("The requested format is not supported"));
    }
}
