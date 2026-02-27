package net.liquidcars.ingestion.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.domain.IngestionReportResponseActionResult;
import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.IngestionReportResponseActionDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IBatchReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    @Qualifier("syncJobLauncher")
    private final JobLauncher jobLauncher;
    private final Job offerIngestionJob;
    private final OfferStreamItemReader offerReader;
    private final IOfferInfraNoSQLService offerInfraNoSQLService;
    private final IOfferInfraSQLService offerInfraSQLService;
    private final IBatchReportInfraSQLService batchReportInfraSQLService;
    private final IReportInfraSQLService iReportInfraSQLService;

    @Value("${ingestion.batch.chunk-size:10}")
    private int chunkSize;

    @Transactional
    @Override
    public void processOffers(IngestionPayloadDto ingestionPayloadDto,
                              UUID inventoryId,
                              UUID requesterParticipantId,
                              IngestionDumpType dumpType,
                              String externalPublicationId) {
        validatePhysicalInventoryExists(inventoryId);
        validateIngestionPayload(ingestionPayloadDto);
        validatePhysicalInventoryHasNotProcessStarted(inventoryId);
        IngestionReportDto ingestionReportDto = createIngestionReportDto(ingestionPayloadDto, inventoryId, requesterParticipantId, dumpType, externalPublicationId);
        ingestionPayloadDto.getOffers().forEach(offerDto -> {
            offerDto.setIngestionReportId(ingestionReportDto.getId());
            this.processOffer(offerDto);
        });
        iReportInfraSQLService.upsertIngestionReport(ingestionReportDto);
        offerInfraKafkaProducerService.sendIngestionJobReport(ingestionReportDto);
    }

    @Transactional
    @Override
    public void processOffersFromUrl(IngestionFormat format,
                                     URI url,
                                     UUID inventoryId,
                                     UUID requesterParticipantId,
                                     IngestionDumpType dumpType,
                                     OffsetDateTime publicationDate,
                                     String externalPublicationId)
    {
        log.info("Triggering remote ingestion from URL: {} with format: {}", url, format);
        validatePhysicalInventoryExists(inventoryId);
        validatePhysicalInventoryHasNotProcessStarted(inventoryId);
        validateUrl(url);
        IOfferParserService parser = getParser(format);

        Thread.ofVirtual().start(() -> {
            try (var pipedOut = new PipedOutputStream();
                 var pipedIn  = new PipedInputStream(pipedOut, 8 * 1024 * 1024)) {

                // Productor: descarga en hilo separado
                Thread downloadThread = Thread.ofVirtual().start(() -> {
                    try (var httpClient = java.net.http.HttpClient.newBuilder()
                            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                            .connectTimeout(java.time.Duration.ofSeconds(10))
                            .build()) {

                        var response = httpClient.send(buildRequest(url),
                                java.net.http.HttpResponse.BodyHandlers.ofInputStream());

                        if (response.statusCode() == 200) {
                            log.info("Download started from URL: {}", url);
                            response.body().transferTo(pipedOut);
                            log.info("Download completed from URL: {}", url);
                        } else {
                            log.error("Failed to download. Status: {}", response.statusCode());
                        }
                    } catch (Exception e) {
                        log.error("Error downloading from {}", url, e);
                    } finally {
                        try { pipedOut.close(); } catch (Exception ignored) {}
                    }
                });

                // Consumidor: batch corre AQUÍ, síncrono, leyendo del pipe
                // mientras el downloadThread escribe en él en paralelo
                runBatchSync(format, parser, pipedIn, inventoryId,
                        requesterParticipantId, externalPublicationId, dumpType, publicationDate);

                // El batch terminó de leer — esperamos que el productor termine también
                downloadThread.join();

            } catch (Exception e) {
                log.error("Critical error during piped ingestion from {}", url, e);
            }
        });
    }

    private IOfferParserService getParser(IngestionFormat format) {
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

    private void validateIngestionPayload(IngestionPayloadDto ingestionPayloadDto) {
        boolean isPayloadEmpty = ingestionPayloadDto == null;

        boolean noOffersToProcess = isPayloadEmpty ||
                (ingestionPayloadDto.getOffers() == null || ingestionPayloadDto.getOffers().isEmpty());

        boolean noOffersToDelete = isPayloadEmpty ||
                (ingestionPayloadDto.getOffersToDelete() == null || ingestionPayloadDto.getOffersToDelete().isEmpty());

        if (noOffersToProcess && noOffersToDelete) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("The payload is empty: No offers to process and no offers to delete")
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

    private void validatePhysicalInventoryExists(UUID inventoryId) {
        if (!iReportInfraSQLService.existsPhysicalInventory(inventoryId)) {
            log.warn("Validation failed: Inventory {} not exists.", inventoryId);

            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message(String.format("Inventory [%s] not exists.", inventoryId))
                    .build();
        }
    }

    private void validatePhysicalInventoryHasNotProcessStarted(UUID inventoryId) {
        List<IngestionBatchStatus> finalStatuses = List.of(
                IngestionBatchStatus.COMPLETED,
                IngestionBatchStatus.FAILED,
                IngestionBatchStatus.ABANDONED,
                IngestionBatchStatus.STOPPED
        );
        if (iReportInfraSQLService.existsByPhysicalInventoryIdAndStatusNotIn(inventoryId, finalStatuses)) {
            log.warn("Validation failed: Inventory {} already has an active ingestion process.", inventoryId);

            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message(String.format("Inventory [%s] already has an active ingestion process in progress. " +
                            "Please wait for it to finish before starting a new one.", inventoryId))
                    .build();
        }
    }

    private static IngestionReportDto createIngestionReportDto(IngestionPayloadDto ingestionPayloadDto,
                                                               UUID inventoryId,
                                                               UUID requesterParticipantId,
                                                               IngestionDumpType dumpType,
                                                               String externalPublicationId) {
        IngestionReportDto ingestionReportDto = createIngestionReportDto(inventoryId, requesterParticipantId, dumpType, IngestionProcessType.PROCESS, externalPublicationId, null, null);
        ingestionReportDto.setReadCount(ingestionPayloadDto.getOffers().size());
        ingestionReportDto.setWriteCount(ingestionPayloadDto.getOffers().size());
        ingestionReportDto.setSkipCount(0);
        ingestionReportDto.setIdsForDelete(ingestionPayloadDto.getOffersToDelete());
        return ingestionReportDto;
    }

    private static IngestionReportDto createIngestionReportDto(UUID inventoryId,
                                                               UUID requesterParticipantId,
                                                               IngestionDumpType dumpType,
                                                               IngestionProcessType ingestionProcessType,
                                                               String externalPublicationId,
                                                               UUID batchJobId,
                                                               OffsetDateTime publicationDate
                                                               ) {
        IngestionReportDto ingestionReportDto = IngestionReportDto.builder().build();
        ingestionReportDto.setId(UUID.randomUUID());
        ingestionReportDto.setProcessType(ingestionProcessType);
        ingestionReportDto.setBatchJobId(batchJobId);
        ingestionReportDto.setRequesterParticipantId(requesterParticipantId);
        ingestionReportDto.setInventoryId(inventoryId);
        ingestionReportDto.setPublicationDate(publicationDate);
        ingestionReportDto.setStatus(IngestionBatchStatus.STARTED);
        ingestionReportDto.setDumpType(dumpType);
        ingestionReportDto.setExternalRequestId(externalPublicationId);

        ingestionReportDto.setProcessed(false);
        ingestionReportDto.setCreatedAt(OffsetDateTime.now());
        ingestionReportDto.setUpdatedAt(OffsetDateTime.now());
        return ingestionReportDto;
    }

    @Transactional
    @Override
    public void processOffersStream(IngestionFormat format, Resource resource, UUID inventoryId,
                                    UUID requesterParticipantId, IngestionDumpType dumpType,
                                    OffsetDateTime publicationDate, String externalPublicationId) {
        validatePhysicalInventoryExists(inventoryId);
        validatePhysicalInventoryHasNotProcessStarted(inventoryId);
        IOfferParserService parser = getParser(format);
        // El hilo orquestador gestiona el ciclo de vida del pipe completo
        Thread.ofVirtual().start(() -> {
            try (var pipedOut = new PipedOutputStream();
                 var pipedIn  = new PipedInputStream(pipedOut, 8 * 1024 * 1024)) {

                // Productor: lee del HTTP request en paralelo
                Thread producerThread = Thread.ofVirtual().start(() -> {
                    try (InputStream inputStream = resource.getInputStream()) {
                        inputStream.transferTo(pipedOut);
                    } catch (Exception e) {
                        log.error("Error transferring HTTP stream to pipe", e);
                    } finally {
                        try { pipedOut.close(); } catch (Exception ignored) {}
                    }
                });

                // Consumidor: batch síncrono AQUÍ — bloquea hasta terminar
                runBatchSync(format, parser, pipedIn, inventoryId,
                        requesterParticipantId, externalPublicationId, dumpType, publicationDate);

                // Batch terminó — esperamos al productor
                producerThread.join();

            } catch (Exception e) {
                log.error("Critical error during stream ingestion", e);
            }
        });
    }

    private JobExecution runBatchSync(IngestionFormat format,
                                      IOfferParserService parser,
                                      InputStream inputStream,
                                      UUID inventoryId,
                                      UUID requesterParticipantId,
                                      String externalPublicationId,
                                      IngestionDumpType dumpType,
                                      OffsetDateTime publicationDate) throws Exception {

        UUID ingestionId = UUID.randomUUID();
        IngestionReportDto ingestionReportDto = createIngestionReportDto(
                inventoryId, requesterParticipantId,
                dumpType, IngestionProcessType.FILE,
                externalPublicationId, ingestionId, publicationDate);

        iReportInfraSQLService.upsertIngestionReport(ingestionReportDto);
        offerInfraKafkaProducerService.sendIngestionJobReport(ingestionReportDto);

        JobDeleteExternalIdsCollector deleteExternalIdsCollector = new JobDeleteExternalIdsCollector();
        offerReader.start(parser, inputStream, deleteExternalIdsCollector);

        JobParameters params = new JobParametersBuilder()
                .addString("ingestionId", ingestionId.toString())
                .addString("ingestionReportId", ingestionReportDto.getId().toString())
                .addString("requesterParticipantId", requesterParticipantId.toString())
                .addString("inventoryId", inventoryId.toString())
                .addString("format", format.name())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // syncJobLauncher bloquea hasta que el job termina
        JobExecution execution = jobLauncher.run(offerIngestionJob, params);
        log.info("Batch finished. Status: {}", execution.getStatus());
        return execution;
    }

    private void processOffer(OfferDto offerDto){
        offerInfraKafkaProducerService.sendOffer(offerDto);
    }

    @Override
    @SchedulerLock(
            name = "IngestionBatchReportsSync_Lock",
            lockAtMostFor = "4m",  // If the pod dies, the lock is released in 4 minutes
            lockAtLeastFor = "1m"  //I hope that if the process is very fast, another replica will catch on right away.
    )
    public void syncPendingBatchReports() {
        List<IngestionBatchReportDto> pendingReports = batchReportInfraSQLService.getBatchPendingReports();
        if (pendingReports.isEmpty()) {
            return;
        }
        log.info("Syncing {} batch pending reports across SQL and NoSQL", pendingReports.size());
        for(IngestionBatchReportDto pendingReport: pendingReports){
            processIngestionBatchReportForCompleteJob(pendingReport);
        }

    }

    @Transactional
    @Override
    public void processIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto) {
        log.info("Received batch ingestion report with id {} for process offers", ingestionBatchReportDto.getJobId());
        //We save the report when we received from kafka topic
        batchReportInfraSQLService.upsertIngestionBatchReport(ingestionBatchReportDto);
        this.processIngestionBatchReportForCompleteJob(ingestionBatchReportDto);
    }

    private void processIngestionBatchReportForCompleteJob(IngestionBatchReportDto ingestionBatchReportDto) {

        long draftOffersCount =
                offerInfraNoSQLService.countOffersFromJobId(ingestionBatchReportDto.getJobId());

        IngestionBatchStatus status = ingestionBatchReportDto.getStatus();
        boolean shouldMarkAsProcessed = false;
        switch (status) {
            case COMPLETED:
                if (ingestionBatchReportDto.getWriteCount() == draftOffersCount) {
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
            batchReportInfraSQLService.upsertIngestionBatchReport(ingestionBatchReportDto);
            IngestionReportDto ingestionReportDto = iReportInfraSQLService.findIngestionReportByBatchJobId(ingestionBatchReportDto.getJobId());
            updateIngestionReportDtoWithBatchReport(ingestionReportDto, ingestionBatchReportDto);
            iReportInfraSQLService.upsertIngestionReport(ingestionReportDto);
            offerInfraKafkaProducerService.sendIngestionJobReport(ingestionReportDto);
        }
    }

    private static void updateIngestionReportDtoWithBatchReport(
            IngestionReportDto ingestionReportDto,
            IngestionBatchReportDto ingestionBatchReportDto) {
        ingestionReportDto.setReadCount((int) ingestionBatchReportDto.getReadCount());
        ingestionReportDto.setWriteCount((int) ingestionBatchReportDto.getWriteCount());
        ingestionReportDto.setSkipCount((int) ingestionBatchReportDto.getSkipCount());
        ingestionReportDto.setFailedExternalIds(ingestionBatchReportDto.getFailedExternalIds());
        ingestionReportDto.setIdsForDelete(ingestionBatchReportDto.getIdsForDelete());
        ingestionReportDto.setStatus(ingestionBatchReportDto.getStatus());
        ingestionReportDto.setUpdatedAt(OffsetDateTime.now());
    }

    @Override
    @SchedulerLock(
            name = "IngestionReportsSync_Lock",
            lockAtMostFor = "4m",  // If the pod dies, the lock is released in 4 minutes
            lockAtLeastFor = "1m"  //I hope that if the process is very fast, another replica will catch on right away.
    )
    public void syncPendingReports() {
        List<IngestionReportDto> pendingReports = iReportInfraSQLService.getPendingReports();
        if (pendingReports.isEmpty()) {
            return;
        }
        log.info("Syncing {} pending reports across SQL and NoSQL", pendingReports.size());
        for(IngestionReportDto pendingReport: pendingReports){
            processIngestionReportForCompleteJob(pendingReport);
        }
    }

    @Override
    @SchedulerLock(
            name = "PromotionPublicationDate_Lock",
            lockAtMostFor = "4m",  // If the pod dies, the lock is released in 4 minutes
            lockAtLeastFor = "1m"  //I hope that if the process is very fast, another replica will catch on right away.
    )
    public void executeDeferredPromotions() {
        OffsetDateTime time = OffsetDateTime.now();
        log.info("Checking for reports ready to be promoted. Current time: {}", time);
        List<IngestionReportDto> pendingReports = iReportInfraSQLService.getPendingPromotionReports(time);
        if (pendingReports.isEmpty()) {
            log.info("No pending promotions found.");
            return;
        }
        log.info("Found {} reports to promote.", pendingReports.size());
        for (IngestionReportDto report : pendingReports) {
            this.promoteDraftOffersToVehicleOffers(report.getId(), true);
        }
    }

    @Transactional
    @Override
    public void processIngestionReport(IngestionReportDto ingestionReportDto) {
        log.info("Received ingestion report with id {} for process offers", ingestionReportDto.getId());
        this.processIngestionReportForCompleteJob(ingestionReportDto);
    }

    private void processIngestionReportForCompleteJob(IngestionReportDto ingestionReportDto) {

        long draftOffersCount =
                offerInfraNoSQLService.countOffersFromReportId(ingestionReportDto.getId());
        boolean shouldMarkAsProcessed = false;
        if (ingestionReportDto.getWriteCount()!= null && ingestionReportDto.getWriteCount() == draftOffersCount) {
            log.info(
                    "Ingestion report with id {} for process offers processed and Success job sent.",
                    ingestionReportDto.getId()
            );
            shouldMarkAsProcessed = true;
        } else {
            log.info(
                    "Ingestion report with id {} not fully processed. Total offers not saved in draft DB. Will retry in next scheduler.",
                    ingestionReportDto.getId()
            );
        }
        if (shouldMarkAsProcessed) {
            if (ingestionReportDto.getProcessType().equals(IngestionProcessType.PROCESS)) { //This case is from json api processing not from a batch file
                ingestionReportDto.setStatus(IngestionBatchStatus.COMPLETED);
            }
            ingestionReportDto.setProcessed(true);
            iReportInfraSQLService.upsertIngestionReport(ingestionReportDto);
            offerInfraKafkaProducerService.sendIngestionJobReport(ingestionReportDto);
        }
    }

    @Override
    public void promoteDraftOffersToVehicleOffers(UUID ingestionReportId, boolean async) {
        log.debug("Calling for start promotion for jobIdentifier: {}", ingestionReportId);
        IngestionReportDto ingestionReportDto = findIngestionReportById(ingestionReportId);
        if(validatePromotion(ingestionReportDto, async)) {
            try {
                List<UUID> activeBookedOfferIds = offerInfraSQLService.findActiveBookedOfferIds(ingestionReportDto.getInventoryId());
                offerInfraNoSQLService.promoteDraftOffersToVehicleOffers(ingestionReportId, ingestionReportDto.getDumpType(),
                        ingestionReportDto.getInventoryId(), ingestionReportDto.getIdsForDelete(), activeBookedOfferIds);
                //Once promotion process is completed we mark job as processed and we update
                ingestionReportDto.setPromoted(true);
                ingestionReportDto.setActiveBookedOfferIds(activeBookedOfferIds);
                iReportInfraSQLService.upsertIngestionReport(ingestionReportDto);
                offerInfraKafkaProducerService.sendIngestionJobReport(ingestionReportDto);
                //When offers are promoted we delete the draft offers of NoSQLDB
                offerInfraNoSQLService.deleteDraftOffersByIngestionReportId(ingestionReportId);
                log.debug("Finish promotion for jobIdentifier: {}", ingestionReportId);
                offerInfraKafkaProducerService.sendIngestionReportPromoteActionNotification(
                        IngestionReportResponseActionDto.builder().ingestionReportId(ingestionReportId).result(IngestionReportResponseActionResult.SUCCESS).build()
                );
            } catch (LCIngestionException e) {
                log.error("Promotion failed for job: {}", ingestionReportId, e);
                offerInfraKafkaProducerService.sendIngestionReportPromoteActionNotification(
                        IngestionReportResponseActionDto.builder()
                                .ingestionReportId(ingestionReportId)
                                .result(IngestionReportResponseActionResult.FAILED)
                                .techCause(e.getTechCause())
                                .errorMsg(e.getMessage())
                                .build()
                );
                if (!async) {
                    throw e;
                }
            }
        }
    }

    private boolean validatePromotion(IngestionReportDto ingestionReportDto, boolean async) {
        return validatePromotionProcessed(ingestionReportDto, async)
                && validatePromotionPromoted(ingestionReportDto, async)
                && validatePromotionByPublishDate(ingestionReportDto, async);
    }

    private boolean validatePromotionProcessed(IngestionReportDto ingestionReportDto, boolean async) {
        if(!ingestionReportDto.isProcessed()){
            String warnMsg = String.format(
                    "Cannot promote offers with ingestionReportId: %s. The report is not process yet.",
                    ingestionReportDto.getId());
            log.warn(warnMsg);
            if(async) {
                offerInfraKafkaProducerService.sendIngestionReportPromoteActionNotification(
                        IngestionReportResponseActionDto.builder()
                                .ingestionReportId(ingestionReportDto.getId())
                                .result(IngestionReportResponseActionResult.FAILED)
                                .techCause(LCTechCauseEnum.INVALID_REQUEST)
                                .errorMsg(warnMsg)
                                .build()
                );
                return false;
            } else {
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.INVALID_REQUEST)
                        .message(warnMsg)
                        .build();
            }
        }
        return true;
    }

    private boolean validatePromotionPromoted(IngestionReportDto ingestionReportDto, boolean async) {
        if(ingestionReportDto.isPromoted()){
            String warnMsg = String.format(
                    "Cannot promote offers with ingestionReportId: %s. The report is not process yet.",
                    ingestionReportDto.getId());
            log.warn(warnMsg);
            if(async) {
                offerInfraKafkaProducerService.sendIngestionReportPromoteActionNotification(
                        IngestionReportResponseActionDto.builder()
                                .ingestionReportId(ingestionReportDto.getId())
                                .result(IngestionReportResponseActionResult.FAILED)
                                .techCause(LCTechCauseEnum.INVALID_REQUEST)
                                .errorMsg(warnMsg)
                                .build()
                );
                return false;
            } else {
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.INVALID_REQUEST)
                        .message(warnMsg)
                        .build();
            }
        }
        return true;
    }

    private boolean validatePromotionByPublishDate(IngestionReportDto ingestionReportDto, boolean async) {
        if(ingestionReportDto.getPublicationDate() != null && ingestionReportDto.getPublicationDate().isAfter(OffsetDateTime.now())){
            String warnMsg = String.format(
                    "Cannot promote offers with ingestionReportId: %s. Promotion is in standby until publication date: %s.",
                    ingestionReportDto.getId(),
                    ingestionReportDto.getPublicationDate());
            log.warn(warnMsg);
            if(async) {
                String msg = "Promotion postponed: current time is before publication date (" + ingestionReportDto.getPublicationDate() + ")";
                log.info(msg);
                offerInfraKafkaProducerService.sendIngestionReportPromoteActionNotification(
                        IngestionReportResponseActionDto.builder()
                                .ingestionReportId(ingestionReportDto.getId())
                                .result(IngestionReportResponseActionResult.FAILED)
                                .techCause(LCTechCauseEnum.DEFERRED_PUBLICATION)
                                .errorMsg(msg)
                                .build()
                );
                return false;
            } else {
                throw LCIngestionException.builder()
                        .techCause(LCTechCauseEnum.INVALID_REQUEST)
                        .message(warnMsg)
                        .build();
            }
        }
        return true;
    }

    @Override
    public void deleteDraftOffersByIngestionReportId(UUID ingestionReportId, boolean async) {
        log.debug("Calling delete draft offers with jobIdentifier: {}", ingestionReportId);
        try {
            offerInfraNoSQLService.deleteDraftOffersByIngestionReportId(ingestionReportId);
            offerInfraKafkaProducerService.sendIngestionReportDeleteActionNotification(
                    IngestionReportResponseActionDto.builder().ingestionReportId(ingestionReportId).result(IngestionReportResponseActionResult.SUCCESS).build()
            );
        } catch (LCIngestionException e) {
            log.error("Delete failed for job: {}", ingestionReportId, e);
            offerInfraKafkaProducerService.sendIngestionReportDeleteActionNotification(
                    IngestionReportResponseActionDto.builder()
                            .ingestionReportId(ingestionReportId)
                            .result(IngestionReportResponseActionResult.FAILED)
                            .techCause(e.getTechCause())
                            .errorMsg(e.getMessage())
                            .build()
            );
            if(!async) {
                throw e;
            }
        }
    }

    @Override
    public IngestionReportPageDto findIngestionReports(IngestionReportFilterDto filter) {
        log.debug("Calling find all ingestion reports with filter: {}", filter);
        return iReportInfraSQLService.findIngestionReports(filter);
    }

    @Override
    public IngestionReportDto findIngestionReportById(UUID ingestionReportId) {
        log.debug("Calling find ingestion report by jobIdentifier: {}", ingestionReportId);
        return iReportInfraSQLService.findIngestionReportById(ingestionReportId);
    }
}
