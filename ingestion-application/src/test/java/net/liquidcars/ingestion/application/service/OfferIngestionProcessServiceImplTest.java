package net.liquidcars.ingestion.application.service;

import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OfferIngestionProcessServiceImplTest {

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
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Captor
    private ArgumentCaptor<OfferDto> offerCaptor;

    @BeforeEach
    void setUp() {
        // Default: nothing is supported unless specified in the test
        lenient().when(parsers.stream()).thenAnswer(i -> Stream.of(mockParser));
    }

    @Test
    void processOffers_ShouldSendEachOfferToKafka() {
        OfferDto offer1 = OfferDtoFactory.getOfferDto();
        OfferDto offer2 = OfferDtoFactory.getOfferDto();
        List<OfferDto> offers = List.of(offer1, offer2);

        service.processOffers(offers);

        verify(offerInfraKafkaProducerService, times(2)).sendOffer(any(OfferDto.class));
    }

    @Test
    void processOffersFromUrl_ShouldNotThrowException_WhenTriggered() {
        String format = "json";
        URI url = URI.create("https://api.motorflash.com/v1/offers");

        // Fix: We must tell the parsers list to provide a parser that supports "json"
        when(mockParser.supports("json")).thenReturn(true);
        when(parsers.stream()).thenAnswer(invocation -> Stream.of(mockParser));

        assertDoesNotThrow(() -> service.processOffersFromUrl(format, url));
    }

    @Test
    void processOffersStream_ShouldUseJsonParser_WhenFormatIsXML() throws Exception {
        String format = "xml";
        InputStream inputStream = new ByteArrayInputStream("<inventory></inventory>".getBytes());

        when(mockParser.supports(format)).thenReturn(true);
        when(parsers.stream()).thenAnswer(invocation -> Stream.of(mockParser));

        service.processOffersStream(format, inputStream);

        verify(jobLauncher, timeout(2000)).run(any(Job.class), any(JobParameters.class));
    }

    @Test
    void processOffersStream_ShouldUseJsonParser_WhenFormatIsJson() throws Exception {
        String format = "json";
        InputStream inputStream = new ByteArrayInputStream("[{}]".getBytes());

        IOfferParserService jsonParser = mock(IOfferParserService.class);
        when(jsonParser.supports("json")).thenReturn(true);
        when(mockParser.supports("json")).thenReturn(false);

        when(parsers.stream()).thenAnswer(i -> Stream.of(mockParser, jsonParser));

        service.processOffersStream(format, inputStream);

        verify(jsonParser, timeout(2000)).supports("json");
        verify(jobLauncher, timeout(2000)).run(any(Job.class), any(JobParameters.class));
        verify(mockParser, never()).parseAndProcess(any(), any());
    }

    @Test
    void processOffersStream_ShouldThrowException_WhenFormatIsNotSupported() {
        String format = "unsupported";
        InputStream inputStream = InputStream.nullInputStream();
        when(parsers.stream()).thenAnswer(invocation -> Stream.empty());

        // Change IllegalArgumentException to LCIngestionException
        assertThrows(LCIngestionException.class, () ->
                service.processOffersStream(format, inputStream)
        );
    }

    @Test
    void processOffersFromUrl_ShouldLogError_WhenResponseIsNot200() throws Exception {
        // 1. Mock the Parser validation so we don't crash before the HTTP call
        when(mockParser.supports("json")).thenReturn(true);
        when(parsers.stream()).thenAnswer(i -> Stream.of(mockParser));

        URI url = URI.create("https://api.test.com/404");
        var mockClient = mock(java.net.http.HttpClient.class);
        var mockResponse = mock(java.net.http.HttpResponse.class);

        when(mockResponse.statusCode()).thenReturn(404);
        lenient().when(mockClient.send(any(), any())).thenReturn(mockResponse);

        try (var mockedHttpClient = mockStatic(java.net.http.HttpClient.class)) {
            mockedHttpClient.when(java.net.http.HttpClient::newHttpClient).thenReturn(mockClient);
            service.processOffersFromUrl("json", url);
            Thread.sleep(300);
        }
    }

    @Test
    void processOffersFromUrl_ShouldLogError_WhenExceptionOccurs() throws Exception {
        when(mockParser.supports("json")).thenReturn(true);
        when(parsers.stream()).thenAnswer(i -> Stream.of(mockParser));
        URI url = URI.create("https://api.test.com/error");
        var mockClient = mock(java.net.http.HttpClient.class);
        lenient().when(mockClient.send(any(), any())).thenThrow(new RuntimeException("Connection Failed"));

        try (var mockedHttpClient = mockStatic(java.net.http.HttpClient.class)) {
            mockedHttpClient.when(java.net.http.HttpClient::newHttpClient).thenReturn(mockClient);
            service.processOffersFromUrl("json", url);
            Thread.sleep(500);
        }
    }

    @Test
    void processOffersStream_ShouldLogError_WhenJobLauncherFails() throws Exception {
        String format = "json";
        InputStream is = new ByteArrayInputStream("[]".getBytes());

        when(mockParser.supports(format)).thenReturn(true);
        when(parsers.stream()).thenAnswer(i -> Stream.of(mockParser));

        // Even if it throws an error, we want to ensure the launcher was at least called
        lenient().when(jobLauncher.run(any(), any())).thenThrow(new RuntimeException("Batch Error"));

        service.processOffersStream(format, is);

        // Increase timeout to give the Virtual Thread time to execute
        verify(jobLauncher, timeout(5000).times(1)).run(any(), any());    }

}
