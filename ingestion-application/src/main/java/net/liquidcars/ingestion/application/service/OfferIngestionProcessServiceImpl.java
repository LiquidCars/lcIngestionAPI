package net.liquidcars.ingestion.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
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

    @Value("${ingestion.batch.chunk-size:10}")
    private int chunkSize;

    @Override
    public void processOffers(List<OfferDto> offers) {
        offers.forEach(this::processOffer);
    }

    @Override
    public void processOffersFromUrl(String format, URI url) {
        log.info("Triggering remote ingestion from URL: {} with format: {}", url, format);

        Thread.ofVirtual().start(() -> {
            try (var httpClient = java.net.http.HttpClient.newHttpClient()) {
                var request = java.net.http.HttpRequest.newBuilder()
                        .uri(url)
                        .GET()
                        .build();

                var response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    this.processOffersStream(format, response.body());
                } else {
                    log.error("Failed to download file from URL: {}. Status code: {}", url, response.statusCode());
                }
            } catch (Exception e) {
                log.error("Error during remote URL ingestion from {}", url, e);
            }
        });
    }

    @Override
    public void processOffersStream(String format, InputStream inputStream) {
        IOfferParserService parser = parsers.stream()
                .filter(p -> p.supports(format))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Format not supported: " + format));

        Thread.ofVirtual().start(() -> {
            try {
                OfferStreamItemReader realReader = new OfferStreamItemReader(parser, inputStream);

                Step dynamicStep = new StepBuilder("ingestionStep-" + format, jobRepository)
                        .<OfferDto, OfferDto>chunk(chunkSize, transactionManager) // Usamos la variable inyectada
                        .reader(realReader)
                        .writer(offerItemWriter)
                        .build();

                Job dynamicJob = new JobBuilder("ingestionJob-" + System.currentTimeMillis(), jobRepository)
                        .start(dynamicStep)
                        .build();

                jobLauncher.run(dynamicJob, new JobParameters());

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
