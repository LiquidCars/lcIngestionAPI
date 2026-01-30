package net.liquidcars.ingestion.application.service;

import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Test
    void processOffers_ShouldSendEachOfferToKafka() {
        OfferDto offer1 = OfferDtoFactory.getOfferDto();
        OfferDto offer2 = new OfferDto();
        List<OfferDto> offers = List.of(offer1, offer2);

        service.processOffers(offers);

        verify(offerInfraKafkaProducerService, times(2)).sendOffer(any(OfferDto.class));
    }

    @Test
    void processOffersFromUrl_ShouldNotThrowException_WhenTriggered() {
        String format = "json";
        URI url = URI.create("https://api.motorflash.com/v1/offers");

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

        assertThrows(IllegalArgumentException.class, () ->
                service.processOffersStream(format, inputStream)
        );
    }



}
