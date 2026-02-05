package net.liquidcars.ingestion.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.application.service.batch.IngestionSkipListener;
import net.liquidcars.ingestion.application.service.batch.JobCompletionNotificationListener;
import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

/**
 * Application service for offer ingestion orchestration.
 * Coordinates between domain logic and infrastructure adapters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferIngestionProcessServiceImpl implements IOfferIngestionProcessService {

    private final List<IOfferParserService> parsers;
    private final IOfferInfraKafkaProducerService offerInfraKafkaProducerService;
    private final OfferItemWriter offerItemWriter;
    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final IngestionSkipListener ingestionSkipListener;
    private final JobCompletionNotificationListener jobCompletionListener;
    private final Job offerIngestionJob;
    private final OfferStreamItemReader offerReader;

    @Value("${ingestion.batch.chunk-size:10}")
    private int chunkSize;

    @Override
    public void processOffers(List<OfferDto> offers) {
        if (offers == null || offers.isEmpty()) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("The offers list is empty or null")
                    .build();
        }
        offers.forEach(this::processOffer);
    }

    @Override
    public void processOffersFromUrl(String format, URI url) {
        log.info("Triggering remote ingestion from URL: {} with format: {}", url, format);
        IOfferParserService parser = getParser(format);
        validateUrl(url);
        Thread.ofVirtual().start(() -> {
            try (var httpClient = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build()) {
                var request = buildRequest(url);

                var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    this.processOffersStream(format, parser, response.body());
                } else {
                    log.error("Failed to download file from URL: {}. Status code: {}", url, response.statusCode());
                }
            } catch (Exception e) {
                log.error("Critical error during remote URL ingestion from {}", url, e);
            }
        });
    }

    private IOfferParserService getParser(String format) {
        return parsers.stream()
                .filter(p -> p.supports(format))
                .findFirst()
                .orElseThrow(() -> LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.INVALID_REQUEST)
                        .message("The requested format is not supported: " + format)
                        .build());
    }

    private java.net.http.HttpRequest buildRequest(URI url) {
        try {
            return java.net.http.HttpRequest.newBuilder()
                    .uri(url)
                    .timeout(java.time.Duration.ofMinutes(5))
                    .header("Accept", "*/*")
                    .GET()
                    .build();
        } catch (IllegalArgumentException e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("Invalid URL or protocol for ingestion: " + url)
                    .cause(e)
                    .build();
        }
    }

    private void validateUrl(URI url) {
        if (url == null || url.getHost() == null) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("The provided URL is malformed or null")
                    .build();
        }
    }

    @Override
    public void processOffersStream(String format, InputStream inputStream) {
        IOfferParserService parser = getParser(format);
        this.processOffersStream(format, parser, inputStream);
    }

    private void processOffersStream(String format, IOfferParserService parser, InputStream inputStream) {
        Thread.ofVirtual().start(() -> {
            try {
                offerReader.start(parser, inputStream);
                JobParameters params = new JobParametersBuilder()
                        .addString("format", format)
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();

                jobLauncher.run(offerIngestionJob, params);

                log.info("Batch job started successfully for format: {}", format);
            } catch (Exception e) {
                log.error("Failed to execute batch job", e);
            }
        });
    }
    private void processOffer(OfferDto offerDto){
        offerInfraKafkaProducerService.sendOffer(offerDto);
    }
}
