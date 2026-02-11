package net.liquidcars.ingestion.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.liquidcars.ingestion.application.service.batch.IngestionSkipListener;
import net.liquidcars.ingestion.application.service.batch.JobCompletionNotificationListener;
import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IBatchReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

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
    private final IOfferInfraSQLService offerInfraSQLService;
    private final IOfferInfraNoSQLService offerInfraNoSQLService;
    private final IBatchReportInfraSQLService batchReportInfraSQLService;

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
            JobExecution execution = null;
            try {
                String ingestionId = UUID.randomUUID().toString();
                offerReader.start(parser, inputStream);
                JobParameters params = new JobParametersBuilder()
                        .addString("ingestionId", ingestionId)
                        .addString("format", format)
                        .addLong("time", System.currentTimeMillis())
                        .toJobParameters();

                execution = jobLauncher.run(offerIngestionJob, params);

                log.info("Batch job started successfully for format: {}", format);
            } catch (Exception e) {
                assert execution != null;
                log.error("Failed to execute batch job: {} . Status: {}", execution.getJobId(), execution.getStatus() , e);
            }
        });
    }
    private void processOffer(OfferDto offerDto){
        offerInfraKafkaProducerService.sendOffer(offerDto);
    }

    @Override
    @SchedulerLock(
            name = "IngestionSync_Lock",
            lockAtMostFor = "4m",  // If the pod dies, the lock is released in 4 minutes
            lockAtLeastFor = "1m"  //I hope that if the process is very fast, another replica will catch on right away.
    )
    public void syncPendingReports() {
        List<IngestionBatchReportDto> pendingReports = batchReportInfraSQLService.getBatchPendingReports();
        if (pendingReports.isEmpty()) {
            return;
        }
        log.info("Syncing {} pending reports across SQL and NoSQL", pendingReports.size());
        for(IngestionBatchReportDto pendingReport: pendingReports){
            processIngestionBatchReportForCompleteJob(pendingReport);
        }

    }

    @Override
    public void processIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto) {
        log.info("Received batch ingestion report with id {} for process offers", ingestionBatchReportDto.getJobId());
        //We save the report when we received from kafka topic
        batchReportInfraSQLService.upsertIngestionReport(ingestionBatchReportDto);
        this.processIngestionBatchReportForCompleteJob(ingestionBatchReportDto);
    }

    private void processIngestionBatchReportForCompleteJob(IngestionBatchReportDto ingestionBatchReportDto) {

        long draftOffersCount =
                offerInfraNoSQLService.getOffersFromJobId(ingestionBatchReportDto.getJobId());

        IngestionBatchStatus status = ingestionBatchReportDto.getStatus();
        boolean shouldMarkAsProcessed = false;
        switch (status) {
            case COMPLETED:
                if (ingestionBatchReportDto.getWriteCount() == draftOffersCount) {
                    // TODO enviar mensaje Kafka aquí
                    log.info(
                            "Batch ingestion report with id {} for process offers processed and Success job sent.",
                            ingestionBatchReportDto.getJobId()
                    );
                    shouldMarkAsProcessed = true;
                } else {
                    log.info(
                            "Batch ingestion report with id {} not fully processed. Total offers not saved in draft DB. Will retry in next scheduler.",
                            ingestionBatchReportDto.getJobId()
                    );
                }
                break;

            case FAILED:
                // TODO enviar mensaje Kafka aquí
                log.info(
                        "Batch ingestion report with id {} processed and Failed job sent.",
                        ingestionBatchReportDto.getJobId()
                );
                shouldMarkAsProcessed = true;
                break;

            default:
                log.warn(
                        "Batch ingestion report with id {} has not processable status {}.",
                        ingestionBatchReportDto.getJobId(),
                        status
                );
                break;
        }
        if (shouldMarkAsProcessed) {
            ingestionBatchReportDto.setProcessed(true);
            batchReportInfraSQLService.upsertIngestionReport(ingestionBatchReportDto);
        }
    }
}
