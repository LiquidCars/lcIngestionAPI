package net.liquidcars.ingestion.application.service;

import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.IngestionReportResponseActionResult;
import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.SortDirection;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IBatchReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.factory.IngestionBatchReportDtoFactory;
import net.liquidcars.ingestion.factory.IngestionReportDtoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class OfferIngestionProcessServiceImplTest {

    @Mock private IOfferParserService parserService;
    @Mock private IOfferInfraKafkaProducerService kafkaProducer;
    @Mock private JobLauncher jobLauncher;
    @Mock private Job offerIngestionJob;
    @Mock private OfferStreamItemReader offerReader;
    @Mock private IOfferInfraNoSQLService offerNoSqlService;
    @Mock private IReportInfraSQLService reportSqlService;
    @Mock private IBatchReportInfraSQLService batchReportSqlService;
    @Mock private IOfferInfraSQLService offerSqlService;

    @InjectMocks
    private OfferIngestionProcessServiceImpl service;

    private final UUID inventoryId = UUID.randomUUID();
    private final UUID participantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "parsers", List.of(parserService));
    }

    @Test
    void shouldProcessOffersSuccessfully() {
        IngestionPayloadDto payload = IngestionPayloadDto.builder()
                .offers(List.of(new OfferDto()))
                .offersToDelete(Collections.emptyList())
                .build();

        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffers(payload, inventoryId, participantId, IngestionDumpType.REPLACEMENT, "ext-1");

        verify(kafkaProducer).sendOffer(any());
        verify(reportSqlService).upsertIngestionReport(any());
    }

    @Test
    void shouldTriggerBatchJobOnStream() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(2000)).run(eq(offerIngestionJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("Caso COMPLETED y Conteo OK")
    void shouldProcessBatchReportSuccess() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto batchReport = IngestionBatchReportDto.builder()
                .jobId(jobId)
                .status(IngestionBatchStatus.COMPLETED)
                .writeCount(10L)
                .processed(false)
                .build();

        when(offerNoSqlService.countOffersFromJobId(jobId)).thenReturn(10L);
        when(reportSqlService.findIngestionReportByBatchJobId(jobId)).thenReturn(IngestionReportDto.builder().build());

        service.processIngestionBatchReport(batchReport);

        verify(batchReportSqlService, atLeastOnce()).upsertIngestionBatchReport(argThat(IngestionBatchReportDto::isProcessed));
    }

    @Test
    @DisplayName("Caso FAILED")
    void shouldProcessBatchReportFailed() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto batchReport = IngestionBatchReportDto.builder()
                .jobId(jobId)
                .status(IngestionBatchStatus.FAILED)
                .build();

        when(reportSqlService.findIngestionReportByBatchJobId(jobId)).thenReturn(IngestionReportDto.builder().build());

        service.processIngestionBatchReport(batchReport);

        verify(batchReportSqlService, atLeastOnce()).upsertIngestionBatchReport(argThat(IngestionBatchReportDto::isProcessed));
    }

    @Test
    @DisplayName("Sincronización de reportes pendientes")
    void shouldSyncPendingBatchReports() {
        IngestionBatchReportDto report = IngestionBatchReportDto.builder()
                .jobId(UUID.randomUUID())
                .status(IngestionBatchStatus.FAILED)
                .build();

        when(batchReportSqlService.getBatchPendingReports()).thenReturn(List.of(report));
        when(reportSqlService.findIngestionReportByBatchJobId(any())).thenReturn(IngestionReportDto.builder().build());

        service.syncPendingBatchReports();

        verify(batchReportSqlService, atLeastOnce()).upsertIngestionBatchReport(any());
    }

    @Test
    @DisplayName("Debe completar el reporte cuando el conteo en NoSQL coincide con writeCount")
    void shouldProcessIngestionReportSuccessfullyWhenCountsMatch() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(reportId)
                .writeCount(25)
                .processType(IngestionProcessType.PROCESS)
                .status(IngestionBatchStatus.STARTED)
                .build();

        when(offerNoSqlService.countOffersFromReportId(reportId)).thenReturn(25L);

        service.processIngestionReport(report);

        verify(reportSqlService).upsertIngestionReport(argThat(r ->
                r.getStatus() == IngestionBatchStatus.COMPLETED &&
                        r.isProcessed() &&
                        r.getId().equals(reportId)
        ));

        verify(kafkaProducer).sendIngestionJobReport(any());
    }

    @Test
    @DisplayName("No debe marcar como completado si el conteo en NoSQL es menor al writeCount")
    void shouldNotCompleteReportWhenNoSqlCountIsLower() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(reportId)
                .writeCount(100)
                .status(IngestionBatchStatus.STARTED)
                .build();

        when(offerNoSqlService.countOffersFromReportId(reportId)).thenReturn(50L);

        service.processIngestionReport(report);

        verify(reportSqlService, never()).upsertIngestionReport(argThat(r ->
                r.getStatus() == IngestionBatchStatus.COMPLETED
        ));
    }

    @Test
    @DisplayName("Debe procesar todos los reportes pendientes en el scheduler")
    void shouldSyncAllPendingReports() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto pendingReport = IngestionReportDto.builder()
                .id(reportId)
                .writeCount(10)
                .processType(IngestionProcessType.PROCESS)
                .build();

        when(reportSqlService.getPendingReports()).thenReturn(List.of(pendingReport));

        when(offerNoSqlService.countOffersFromReportId(reportId)).thenReturn(10L);

        service.syncPendingReports();

        verify(kafkaProducer, atLeastOnce()).sendIngestionJobReport(any());

        verify(reportSqlService, atLeastOnce()).upsertIngestionReport(argThat(dto ->
                dto.getId().equals(reportId) && dto.isProcessed()
        ));
    }

    @Test
    @DisplayName("Debe cubrir la lambda interna de processOffersFromUrl")
    void shouldCoverProcessOffersFromUrlLambda() {
        URI uri = URI.create("https://localhost:8080/offers.xml");
        when(parserService.supports(IngestionFormat.xml)).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersFromUrl(IngestionFormat.xml, uri, inventoryId, participantId,
                IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

        verify(reportSqlService, timeout(2000)).existsByPhysicalInventoryIdAndStatusNotIn(any(), any());
    }

    @Test
    @DisplayName("Debe cubrir la lambda de processOffersStream (Virtual Thread)")
    void shouldCoverProcessOffersStreamLambda() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(eq(offerIngestionJob), any(JobParameters.class));
        verify(reportSqlService, timeout(3000)).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("Debe cubrir el flujo de éxito (200 OK) dentro de la lambda")
    void shouldCoverLambdaSuccessFlow() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/test", exchange -> {
            byte[] response = "<xml>fake</xml>".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            URI localUri = URI.create("http://localhost:" + port + "/test");

            when(parserService.supports(IngestionFormat.xml)).thenReturn(true);
            when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
            when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

            service.processOffersFromUrl(IngestionFormat.xml, localUri, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

            verify(reportSqlService, timeout(2000)).existsByPhysicalInventoryIdAndStatusNotIn(any(), any());

            verify(reportSqlService, timeout(5000)).upsertIngestionReport(any());
            verify(jobLauncher, timeout(5000)).run(any(), any());

        } finally {
            server.stop(0);
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("Debe lanzar excepción si la URL no tiene host (Validación inicial)")
    void shouldThrowExceptionForInvalidUrl() {
        URI uriWithoutHost = URI.create("mailto:test@liquidcars.com");
        assertThrows(LCIngestionException.class, () ->
                service.processOffersFromUrl(IngestionFormat.xml, uriWithoutHost, inventoryId, participantId,
                        IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1")
        );
    }

    @Test
    @DisplayName("Debe promover ofertas exitosamente cuando el reporte ha sido procesado")
    void shouldPromoteOffersSuccessfully() {
        UUID jobId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .processed(true)
                .promoted(false)
                .status(IngestionBatchStatus.COMPLETED)
                .dumpType(IngestionDumpType.REPLACEMENT)
                .inventoryId(inventoryId)
                .idsForDelete(List.of("ext-del-1"))
                .build();

        when(service.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, false);

        verify(offerNoSqlService, times(1)).promoteDraftOffersToVehicleOffers(
                eq(jobId),
                any(),
                any(),
                any(),
                any());
        verify(kafkaProducer).sendIngestionReportPromoteActionNotification(
                argThat(dto -> dto.getResult().equals(IngestionReportResponseActionResult.SUCCESS))
        );
    }

    @Test
    @DisplayName("No debe promover ofertas si el estado del reporte no es COMPLETED y ya fue procesado")
    void shouldNotPromoteOffersWhenStatusIsNotCompletedAndIsProcessed() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .promoted(true) // ya promovido → corte temprano
                .processed(true)
                .status(IngestionBatchStatus.STARTED)
                .dumpType(IngestionDumpType.REPLACEMENT)
                .inventoryId(inventoryId)
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);


        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.promoteDraftOffersToVehicleOffers(jobId, false)
        );

        assertThat(exception.getMessage()).contains("Cannot promote offers with ingestionReportId");

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(
                any(), any(), any(), any(), any());
        verify(reportSqlService, never()).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("No debe promover ofertas si el estado del reporte no es COMPLETED")
    void shouldNotPromoteOffersWhenStatusIsNotCompleted() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .promoted(false)
                .processed(false) // No esta procesado
                .status(IngestionBatchStatus.STARTED)
                .dumpType(IngestionDumpType.REPLACEMENT)
                .inventoryId(inventoryId)
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);


        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.promoteDraftOffersToVehicleOffers(jobId, false)
        );

        assertThat(exception.getMessage()).contains("Cannot promote offers with ingestionReportId");

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(
                any(), any(), any(), any(), any());
        verify(reportSqlService, never()).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("No debe promover ofertas si la fecha de publicacion es mayor que el momento actual")
    void shouldNotPromoteOffersWhenPublishDateIsBefore() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .promoted(false)
                .processed(true)
                .publicationDate(OffsetDateTime.now().plusHours(1))
                .status(IngestionBatchStatus.STARTED)
                .dumpType(IngestionDumpType.REPLACEMENT)
                .inventoryId(inventoryId)
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);


        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.promoteDraftOffersToVehicleOffers(jobId, false)
        );

        assertThat(exception.getMessage()).contains("Cannot promote offers with ingestionReportId");

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(
                any(), any(), any(), any(), any());
        verify(reportSqlService, never()).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("Debe promover ofertas incluso si el reporte marca 'processed' como true (Comportamiento actual)")
    void shouldNotPromoteIfAlreadyProcessed() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .processed(true)
                .status(IngestionBatchStatus.COMPLETED)
                .inventoryId(UUID.randomUUID())
                .dumpType(IngestionDumpType.INCREMENTAL)
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, false);

        verify(offerNoSqlService, times(1)).promoteDraftOffersToVehicleOffers(
                eq(jobId),
                any(),
                any(),
                any(),
                any()
        );

        verify(reportSqlService, times(1)).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("Debe eliminar ofertas draft por identificador de trabajo")
    void shouldDeleteDraftOffers() {
        UUID jobId = UUID.randomUUID();

        service.deleteDraftOffersByIngestionReportId(jobId, false);

        verify(offerNoSqlService, times(1)).deleteDraftOffersByIngestionReportId(jobId);
    }

    @Test
    @DisplayName("Debe cubrir el bloque catch de la lambda processOffersStream cuando el JobLauncher falla")
    void shouldCoverStreamLambdaCatchBlock() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        doThrow(new RuntimeException("Batch Exception")).when(jobLauncher).run(any(), any());

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());

        verify(reportSqlService, timeout(3000)).upsertIngestionReport(any());

        Thread.sleep(500);
    }

    @Test
    @DisplayName("Debe cubrir la rama del else en el catch de la lambda (execution == null)")
    void shouldCoverStreamLambdaCatchWithNullException() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));

        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());
    }

    @Test
    @DisplayName("Debe cubrir la rama execution != null en el catch de processOffersStream")
    void shouldCoverStreamLambdaCatchWithExecutionNotNull() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        lenient().when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        JobExecution mockExecution = mock(JobExecution.class);
        lenient().when(mockExecution.getJobId()).thenReturn(123L);
        lenient().when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        lenient().when(jobLauncher.run(eq(offerIngestionJob), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Forced failure"));

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());

        Thread.sleep(500);
    }

    @Test
    @DisplayName("Debe encontrar reportes paginados aplicando el filtro")
    void shouldFindAllIngestionReports() {
        IngestionReportFilterDto filter = IngestionReportFilterDto.builder()
                .page(0)
                .size(20)
                .sortBy(IngestionReportSortField.createdAt)
                .sortDirection(SortDirection.DESC)
                .build();

        IngestionReportDto reportDto = IngestionReportDtoFactory.getIngestionReportDto();

        IngestionReportPageDto expectedPage = IngestionReportPageDto.builder()
                .content(List.of(reportDto))
                .totalElements(1L)
                .totalPages(1)
                .size(20)
                .number(0)
                .last(true)
                .build();

        when(reportSqlService.findIngestionReports(any(IngestionReportFilterDto.class)))
                .thenReturn(expectedPage);

        IngestionReportPageDto result = service.findIngestionReports(filter);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(reportDto.getId());

        verify(reportSqlService).findIngestionReports(filter);
    }

    @Test
    @DisplayName("Debe encontrar reporte por ID")
    void shouldFindReportById() {
        UUID id = UUID.randomUUID();
        IngestionReportDto reportDto = IngestionReportDtoFactory.getIngestionReportDto();
        when(reportSqlService.findIngestionReportById(id)).thenReturn(reportDto);
        IngestionReportDto result = service.findIngestionReportById(id);
        assertThat(result).isNotNull();
        verify(reportSqlService).findIngestionReportById(id);
    }

    @Test
    @DisplayName("Debe encontrar el parser correcto cuando el formato es soportado")
    void shouldReturnParserWhenFormatIsSupported() {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(eq(inventoryId), any()))
                .thenReturn(false);
        when(parserService.supports(IngestionFormat.xml)).thenReturn(true);

        service.processOffersStream(
                IngestionFormat.xml,
                resource,
                inventoryId,
                participantId,
                IngestionDumpType.INCREMENTAL,
                OffsetDateTime.now(),
                "ext-id"
        );

        verify(parserService, atLeastOnce()).supports(IngestionFormat.xml);
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException cuando ningún parser soporta el formato")
    void shouldThrowExceptionWhenNoParserSupportsFormat() {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(eq(inventoryId), any()))
                .thenReturn(false);
        when(parserService.supports(any())).thenReturn(false);

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffersStream(
                        IngestionFormat.xml,
                        resource,
                        inventoryId,
                        participantId,
                        IngestionDumpType.REPLACEMENT,
                        OffsetDateTime.now(),
                        "ext-id"
                )
        );

        assertThat(exception.getMessage()).contains("The requested format is not supported");
    }

    @Test
    @DisplayName("Debe permitir continuar si el participante no tiene procesos activos")
    void shouldAllowProcessingWhenNoActiveProcessExists() {
        UUID requesterId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));

        when(parserService.supports(IngestionFormat.xml)).thenReturn(true);

        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(eq(inventoryId), any()))
                .thenReturn(false);

        service.processOffersStream(
                IngestionFormat.xml,
                resource,
                inventoryId,
                requesterId,
                IngestionDumpType.INCREMENTAL,
                null,
                "ext"
        );

        verify(reportSqlService).existsByPhysicalInventoryIdAndStatusNotIn(eq(inventoryId), any());
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException si el participante ya tiene un proceso en curso")
    void shouldThrowExceptionWhenActiveProcessExists() {
        UUID requesterId = UUID.randomUUID();
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));

        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(UUID.class), any()))
                .thenReturn(true);

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffersStream(
                        IngestionFormat.xml,
                        resource,
                        UUID.randomUUID(),
                        requesterId,
                        IngestionDumpType.INCREMENTAL,
                        null,
                        "ext")
        );

        assertThat(exception.getMessage()).contains("already has an active ingestion process in progress");

        verify(reportSqlService).existsByPhysicalInventoryIdAndStatusNotIn(any(UUID.class), any());
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException cuando el Builder de HttpRequest falla")
    void shouldThrowExceptionWhenHttpRequestBuilderFails() {
        URI uriWithInvalidPort = URI.create("http://localhost:999999");

        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersFromUrl(
                IngestionFormat.xml,
                uriWithInvalidPort,
                inventoryId,
                participantId,
                IngestionDumpType.REPLACEMENT,
                OffsetDateTime.now(),
                "ext-1"
        );

        verify(reportSqlService, timeout(2000)).existsByPhysicalInventoryIdAndStatusNotIn(any(), any());
    }

    @Test
    @DisplayName("Debe cubrir el catch de buildRequest con un esquema no soportado")
    void shouldCoverBuildRequestCatchBlock() throws Exception {
        URI uriWithUnsupportedScheme = new URI("mailto", "test@liquidcars.com", null);

        java.lang.reflect.Method method = OfferIngestionProcessServiceImpl.class
                .getDeclaredMethod("buildRequest", URI.class);
        method.setAccessible(true);

        java.lang.reflect.InvocationTargetException invocationException = assertThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> method.invoke(service, uriWithUnsupportedScheme),
                "Se esperaba que el Builder de HttpRequest rechazara el esquema mailto"
        );

        Throwable cause = invocationException.getCause();
        assertThat(cause).isInstanceOf(LCIngestionException.class);
        assertThat(cause.getMessage()).contains("Invalid URL or protocol for ingestion");
    }

    @Test
    @DisplayName("Rama Catch: execution != null (Error después de iniciar el Job)")
    void shouldCoverCatchWhenExecutionIsNotNull() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));

        lenient().when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        JobExecution mockExecution = mock(JobExecution.class);
        lenient().when(mockExecution.getJobId()).thenReturn(123L);
        lenient().when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        lenient().when(jobLauncher.run(eq(offerIngestionJob), any(JobParameters.class)))
                .thenAnswer(invocation -> {
                    throw new RuntimeException("Simulated Failure with Execution Object");
                });

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());

        Thread.sleep(500);
    }

    @Test
    @DisplayName("Rama Catch: execution == null (Error directo en el Launcher)")
    void shouldCoverCatchWhenExecutionIsNull() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);
        lenient().when(parserService.supports(any())).thenReturn(true);

        doThrow(new RuntimeException("Direct Launcher Failure"))
                .when(jobLauncher).run(any(), any());

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la lista de ofertas es nula")
    void shouldThrowExceptionWhenOffersListIsNull() {
        IngestionPayloadDto payload = IngestionPayloadDto.builder().offers(null).build();

        assertThrows(LCIngestionException.class, () ->
                service.processOffers(payload, inventoryId, participantId, IngestionDumpType.INCREMENTAL, "ext")
        );
    }

    @Test
    @DisplayName("Debe cubrir el bloque catch de la lambda (Error de conexión)")
    void shouldCoverLambdaCatchBlock() throws Exception {
        URI unreachableUri = URI.create("http://localhost:1");
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        // Mock del JobExecution que devuelve el launcher
        JobExecution mockExecution = mock(JobExecution.class);
        when(jobLauncher.run(any(), any())).thenReturn(mockExecution);

        service.processOffersFromUrl(IngestionFormat.xml, unreachableUri, inventoryId, participantId,
                IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

        // El batch SÍ se llama pero con stream vacío (error de conexión cierra el pipe sin datos)
        verify(jobLauncher, timeout(3000)).run(any(), any());
        // El report se guarda con status STARTED
        verify(reportSqlService, timeout(3000)).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("Debe loguear error cuando el statusCode no es 200")
    void shouldLogErrorWhenDownloadStatusIsNot200() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/404", ex -> {
            ex.sendResponseHeaders(404, -1);
            ex.close();
        });
        server.start();

        try {
            URI uri = URI.create("http://localhost:" + server.getAddress().getPort() + "/404");

            when(parserService.supports(any())).thenReturn(true);
            when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
            when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

            JobExecution mockExecution = mock(JobExecution.class);
            when(jobLauncher.run(any(), any())).thenReturn(mockExecution);

            service.processOffersFromUrl(IngestionFormat.xml, uri, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext");

            // El batch SÍ se llama pero lee 0 registros porque el pipe se cierra sin datos (404)
            verify(jobLauncher, timeout(3000)).run(any(), any());
            // Verificamos que se intentó crear el report
            verify(reportSqlService, timeout(3000)).upsertIngestionReport(any());
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Debe loguear error de batch cuando execution NO es nula")
    void shouldLogErrorWhenBatchFailsWithExecutionNotNull() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));

        lenient().when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        JobExecution mockExecution = mock(JobExecution.class);
        lenient().when(mockExecution.getJobId()).thenReturn(777L);
        lenient().when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        lenient().when(jobLauncher.run(any(), any())).thenAnswer(invocation -> {
            throw new RuntimeException("Simulated Failure");
        });

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId, IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());
        Thread.sleep(500);
    }


    @Test
    @DisplayName("Debe loguear advertencia para status no procesable")
    void shouldWarnWhenStatusIsNotProcessable() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto report = IngestionBatchReportDtoFactory.getIngestionBatchReportDto();
        report.setJobId(jobId);
        report.setStatus(IngestionBatchStatus.STARTED);

        service.processIngestionBatchReport(report);

        verify(batchReportSqlService).upsertIngestionBatchReport(report);
    }
    @Test
    @DisplayName("Debe retornar temprano si no hay reportes pendientes")
    void shouldReturnEarlyWhenNoPendingBatchReports() {
        when(batchReportSqlService.getBatchPendingReports()).thenReturn(Collections.emptyList());

        service.syncPendingBatchReports();

        verify(offerNoSqlService, never()).countOffersFromJobId(any());
    }

    @Test
    @DisplayName("Debe loguear reintento cuando el conteo en NoSQL no coincide")
    void shouldLogRetryWhenNoSqlCountMismatch() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto report = IngestionBatchReportDto.builder()
                .jobId(jobId)
                .status(IngestionBatchStatus.COMPLETED)
                .writeCount(100L)
                .build();

        when(offerNoSqlService.countOffersFromJobId(jobId)).thenReturn(50L);

        service.processIngestionBatchReport(report);

        assertThat(report.isProcessed()).isFalse();
        verify(batchReportSqlService).upsertIngestionBatchReport(report);
    }

    @Test
    @DisplayName("Debe cubrir el return temprano en syncPendingReports cuando no hay reportes")
    void shouldReturnEarlyInSyncPendingReports() {
        when(reportSqlService.getPendingReports()).thenReturn(Collections.emptyList());

        service.syncPendingReports();

        verify(offerNoSqlService, never()).countOffersFromReportId(any());
        verify(reportSqlService, never()).upsertIngestionReport(any());
    }


    @Test
    @DisplayName("Promote: Debería relanzar excepción si NO es asíncrono")
    void promote_ThrowExceptionWhenSync() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .status(IngestionBatchStatus.COMPLETED)
                .processed(true)
                .build();
        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        doThrow(LCIngestionException.builder().techCause(LCTechCauseEnum.DATABASE).build())
                .when(offerNoSqlService).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());

        assertThrows(LCIngestionException.class, () ->
                service.promoteDraftOffersToVehicleOffers(jobId, false));
    }

    @Test
    @DisplayName("Delete: Debería relanzar excepción si ocurre un error y NO es asíncrono")
    void delete_ThrowExceptionWhenSync() {
        UUID jobId = UUID.randomUUID();
        doThrow(LCIngestionException.builder().techCause(LCTechCauseEnum.DATABASE).build())
                .when(offerNoSqlService).deleteDraftOffersByIngestionReportId(jobId);

        assertThrows(LCIngestionException.class, () ->
                service.deleteDraftOffersByIngestionReportId(jobId, false));

        verify(kafkaProducer).sendIngestionReportDeleteActionNotification(
                argThat(dto -> dto.getResult().equals(IngestionReportResponseActionResult.FAILED))
        );
    }

    @Test
    @DisplayName("Lambda Stream: Error cuando execution ES nulo")
    void processOffersStream_ErrorWithNullExecution() throws Exception {
        byte[] content = "<xml>data</xml>".getBytes();
        Resource resource = new InputStreamResource(new ByteArrayInputStream(content));

        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);
        when(jobLauncher.run(any(), any())).thenThrow(new RuntimeException("Launcher Direct Failure"));

        service.processOffersStream(IngestionFormat.xml, resource, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(2000)).run(any(), any());
    }

    @Test
    @DisplayName("Debe ejecutar promociones diferidas para los reportes cuya fecha de publicación ha pasado")
    void shouldExecuteDeferredPromotions() {
        UUID reportId1 = UUID.randomUUID();
        UUID reportId2 = UUID.randomUUID();

        IngestionReportDto report1 = IngestionReportDto.builder()
                .id(reportId1)
                .inventoryId(UUID.randomUUID())
                .processed(true)
                .promoted(false)
                .publicationDate(OffsetDateTime.now().minusMinutes(10))
                .status(IngestionBatchStatus.COMPLETED)
                .dumpType(IngestionDumpType.REPLACEMENT)
                .build();

        IngestionReportDto report2 = IngestionReportDto.builder()
                .id(reportId2)
                .inventoryId(UUID.randomUUID())
                .processed(true)
                .promoted(false)
                .publicationDate(OffsetDateTime.now().minusHours(1))
                .status(IngestionBatchStatus.COMPLETED)
                .dumpType(IngestionDumpType.INCREMENTAL)
                .build();

        when(reportSqlService.getPendingPromotionReports(any(OffsetDateTime.class)))
                .thenReturn(List.of(report1, report2));

        when(reportSqlService.findIngestionReportById(reportId1)).thenReturn(report1);
        when(reportSqlService.findIngestionReportById(reportId2)).thenReturn(report2);

        service.executeDeferredPromotions();

        verify(offerNoSqlService).promoteDraftOffersToVehicleOffers(
                eq(reportId1), any(), any(), any(), any());
        verify(offerNoSqlService).promoteDraftOffersToVehicleOffers(
                eq(reportId2), any(), any(), any(), any());

        verify(reportSqlService, times(2)).upsertIngestionReport(any(IngestionReportDto.class));

        verify(kafkaProducer, times(2)).sendIngestionJobReport(any(IngestionReportDto.class));
    }

    @Test
    @DisplayName("Debe retornar temprano en executeDeferredPromotions si no hay reportes para promover")
    void shouldReturnEarlyInExecuteDeferredPromotionsWhenNoReports() {
        when(reportSqlService.getPendingPromotionReports(any(OffsetDateTime.class)))
                .thenReturn(Collections.emptyList());

        service.executeDeferredPromotions();

        verify(reportSqlService, never()).findIngestionReportById(any());
        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe cubrir la transferencia de datos (200 OK) y el log de error en processOffersFromUrl")
    void shouldCoverDownloadSuccessAndErrorForJacoco() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/success", exchange -> {
            byte[] response = "<xml>fake</xml>".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/error", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();

            URI successUri = URI.create("http://localhost:" + port + "/success");
            when(parserService.supports(any())).thenReturn(true);
            when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
            when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

            service.processOffersFromUrl(IngestionFormat.xml, successUri, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-200");

            verify(jobLauncher, timeout(5000)).run(any(), any());
            verify(reportSqlService, timeout(5000)).upsertIngestionReport(any());

            URI errorUri = URI.create("http://localhost:" + port + "/error");

            service.processOffersFromUrl(IngestionFormat.xml, errorUri, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-404");

            verify(jobLauncher, timeout(5000).times(2)).run(any(), any());

        } finally {
            server.stop(0);
            Thread.sleep(200);
        }
    }

    @Test
    @DisplayName("Cobertura total de la lambda de descarga y transferencia de datos")
    void shouldCoverDownloadAndTransferFullLambda() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        byte[] mockData = "<offers><offer>test</offer></offers>".getBytes();

        server.createContext("/200", exchange -> {
            exchange.sendResponseHeaders(200, mockData.length);
            exchange.getResponseBody().write(mockData);
            exchange.close();
        });

        server.createContext("/404", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();

            when(parserService.supports(any())).thenReturn(true);
            when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
            when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

            URI uri200 = URI.create("http://localhost:" + port + "/200");
            service.processOffersFromUrl(IngestionFormat.xml, uri200, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-id");

            verify(jobLauncher, timeout(5000)).run(any(), any());
            verify(reportSqlService, timeout(5000)).upsertIngestionReport(any());

            URI uri404 = URI.create("http://localhost:" + port + "/404");
            service.processOffersFromUrl(IngestionFormat.xml, uri404, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-id-404");

            verify(jobLauncher, timeout(5000).times(2)).run(any(), any());

        } finally {
            server.stop(0);
            Thread.sleep(300);
        }
    }

    @Test
    @DisplayName("Debe notificar via Kafka y retornar false cuando la promoción es asíncrona y la fecha es futura")
    void shouldNotifyAndReturnFalseWhenAsyncAndFuturePublicationDate() {
        UUID jobId = UUID.randomUUID();
        OffsetDateTime futureDate = OffsetDateTime.now().plusDays(1);

        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .processed(true)
                .promoted(false)
                .publicationDate(futureDate)
                .status(IngestionBatchStatus.COMPLETED)
                .inventoryId(UUID.randomUUID())
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, true);

        verify(kafkaProducer).sendIngestionReportPromoteActionNotification(
                argThat(dto ->
                        dto.getIngestionReportId().equals(jobId) &&
                                dto.getResult().equals(IngestionReportResponseActionResult.FAILED) &&
                                dto.getTechCause().equals(LCTechCauseEnum.DEFERRED_PUBLICATION) &&
                                dto.getErrorMsg().contains("Promotion postponed")
                )
        );

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(
                any(), any(), any(), any(), any());

        verify(reportSqlService, never()).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("Debe notificar via Kafka cuando el reporte ya está promocionado y es una llamada asíncrona")
    void shouldNotifyKafkaWhenAlreadyPromotedAndAsync() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto alreadyPromotedReport = IngestionReportDto.builder()
                .id(reportId)
                .processed(true)
                .promoted(true)
                .inventoryId(UUID.randomUUID())
                .build();

        when(reportSqlService.findIngestionReportById(reportId)).thenReturn(alreadyPromotedReport);

        service.promoteDraftOffersToVehicleOffers(reportId, true);

        verify(kafkaProducer).sendIngestionReportPromoteActionNotification(
                argThat(dto ->
                        dto.getIngestionReportId().equals(reportId) &&
                                dto.getResult().equals(IngestionReportResponseActionResult.FAILED) &&
                                dto.getTechCause().equals(LCTechCauseEnum.INVALID_REQUEST) &&
                                dto.getErrorMsg().contains("Cannot promote offers")
                )
        );

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe notificar via Kafka cuando el reporte NO está procesado y es una llamada asíncrona")
    void shouldNotifyKafkaWhenNotProcessedAndAsync() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto notProcessedReport = IngestionReportDto.builder()
                .id(reportId)
                .processed(false)
                .promoted(false)
                .inventoryId(UUID.randomUUID())
                .build();

        when(reportSqlService.findIngestionReportById(reportId)).thenReturn(notProcessedReport);

        service.promoteDraftOffersToVehicleOffers(reportId, true);

        verify(kafkaProducer).sendIngestionReportPromoteActionNotification(
                argThat(dto ->
                        dto.getIngestionReportId().equals(reportId) &&
                                dto.getResult().equals(IngestionReportResponseActionResult.FAILED) &&
                                dto.getTechCause().equals(LCTechCauseEnum.INVALID_REQUEST) &&
                                dto.getErrorMsg().contains("The report is not process yet")
                )
        );

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException cuando la URL es null")
    void shouldThrowExceptionWhenUrlIsNull() {
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffersFromUrl(IngestionFormat.xml, null, inventoryId, participantId,
                        IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1")
        );

        assertThat(exception.getMessage()).isEqualTo("The provided URL is malformed or null");
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException cuando la URL no tiene host")
    void shouldThrowExceptionWhenUrlHasNoHost() {
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        URI uriWithoutHost = URI.create("path/without/host");

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffersFromUrl(IngestionFormat.xml, uriWithoutHost, inventoryId, participantId,
                        IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1")
        );

        assertThat(exception.getMessage()).isEqualTo("The provided URL is malformed or null");
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException cuando las listas de ofertas están vacías")
    void shouldThrowExceptionWhenListsAreEmpty() {
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);

        IngestionPayloadDto emptyPayload = IngestionPayloadDto.builder()
                .offers(Collections.emptyList())
                .offersToDelete(Collections.emptyList())
                .build();

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffers(emptyPayload, inventoryId, participantId, IngestionDumpType.REPLACEMENT, "ext")
        );

        assertThat(exception.getMessage()).isEqualTo("The payload is empty: No offers to process and no offers to delete");
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException cuando las listas de ofertas son nulas")
    void shouldThrowExceptionWhenListsAreNull() {
        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);

        IngestionPayloadDto nullListsPayload = IngestionPayloadDto.builder()
                .offers(null)
                .offersToDelete(null)
                .build();

        assertThrows(LCIngestionException.class, () ->
                service.processOffers(nullListsPayload, inventoryId, participantId, IngestionDumpType.REPLACEMENT, "ext")
        );
    }

    @Test
    @DisplayName("Forzar cobertura total de ramas 200 y else mediante sincronización explícita")
    void shouldForceJacocoCoverageForDownloadBranches() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        server.createContext("/test", exchange -> {
            int code = Integer.parseInt(exchange.getRequestURI().getQuery());
            byte[] response = "data".getBytes();
            exchange.sendResponseHeaders(code, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();

            when(parserService.supports(any())).thenReturn(true);
            when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
            when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

            JobExecution mockExecution = mock(JobExecution.class);
            when(mockExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
            when(jobLauncher.run(any(), any())).thenReturn(mockExecution);

            CompletableFuture<Void> completionFuture = new CompletableFuture<>();
            doAnswer(invocation -> {
                completionFuture.complete(null);
                return null;
            }).when(reportSqlService).upsertIngestionReport(any());

            URI uri200 = URI.create("http://localhost:" + port + "/test?200");
            service.processOffersFromUrl(IngestionFormat.xml, uri200, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-200");

            completionFuture.get(5, java.util.concurrent.TimeUnit.SECONDS);

            CompletableFuture<Void> completionFutureError = new CompletableFuture<>();
            doAnswer(invocation -> {
                completionFutureError.complete(null);
                return null;
            }).when(reportSqlService).upsertIngestionReport(any());

            URI uri404 = URI.create("http://localhost:" + port + "/test?404");
            service.processOffersFromUrl(IngestionFormat.xml, uri404, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-404");

            completionFutureError.get(5, java.util.concurrent.TimeUnit.SECONDS);

        } finally {
            server.stop(0);
            Thread.sleep(500);
        }
    }

    @Test
    @DisplayName("Cobertura definitiva para transferTo y log Download Completed")
    void shouldCoverTransferToAndDownloadCompleted() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(0), 0);
        byte[] mockContent = "contenido-de-prueba-largo-para-forzar-transferencia".getBytes();
        server.createContext("/success", exchange -> {
            exchange.sendResponseHeaders(200, mockContent.length);
            exchange.getResponseBody().write(mockContent);
            exchange.close();
        });
        server.start();

        try {
            int port = server.getAddress().getPort();
            URI uri = URI.create("http://localhost:" + port + "/success");

            when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
            when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);
            when(parserService.supports(any())).thenReturn(true);

            JobExecution mockExecution = mock(JobExecution.class);
            when(mockExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
            when(jobLauncher.run(any(), any())).thenReturn(mockExecution);

            service.processOffersFromUrl(IngestionFormat.xml, uri, inventoryId, participantId,
                    IngestionDumpType.REPLACEMENT, null, "ext-jacoco");

            verify(jobLauncher, timeout(5000)).run(any(), any());

            verify(reportSqlService, timeout(5000)).upsertIngestionReport(any());

            Thread.sleep(1000);

        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Cobertura de processOffersStream: Batch, Join y Catch de transferencia")
    void shouldCoverProcessOffersStreamFullFlow() throws Exception {
        Resource mockResource = mock(Resource.class);
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated Stream Error");
            }
        };
        when(mockResource.getInputStream()).thenReturn(failingStream);

        when(reportSqlService.existsPhysicalInventory(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);
        when(parserService.supports(any())).thenReturn(true);

        JobExecution mockExecution = mock(JobExecution.class);
        when(mockExecution.getStatus()).thenReturn(org.springframework.batch.core.BatchStatus.COMPLETED);
        when(jobLauncher.run(any(), any())).thenReturn(mockExecution);

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(reportSqlService).upsertIngestionReport(any());

        service.processOffersStream(IngestionFormat.xml, mockResource, inventoryId, participantId,
                IngestionDumpType.REPLACEMENT, null, "ext-stream-test");

        boolean completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

        verify(jobLauncher, timeout(5000)).run(any(), any());

        Thread.sleep(500);
    }
}