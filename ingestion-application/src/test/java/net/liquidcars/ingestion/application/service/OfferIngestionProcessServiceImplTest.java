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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
        ReflectionTestUtils.setField(service, "chunkSize", 10);
    }

    @Test
    void shouldProcessOffersSuccessfully() {
        IngestionPayloadDto payload = IngestionPayloadDto.builder()
                .offers(List.of(new OfferDto()))
                .offersToDelete(Collections.emptyList())
                .build();

        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffers(payload, inventoryId, participantId, IngestionDumpType.REPLACEMENT, "ext-1");

        verify(kafkaProducer).sendOffer(any());
        verify(reportSqlService).upsertIngestionReport(any());
    }

    @Test
    void shouldTriggerBatchJobOnStream() throws Exception {
        InputStream stream = new ByteArrayInputStream("data".getBytes());
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersStream(IngestionFormat.xml, stream, inventoryId, participantId,
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
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersFromUrl(IngestionFormat.xml, uri, inventoryId, participantId,
                IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

        verify(reportSqlService, timeout(2000)).existsByPhysicalInventoryIdAndStatusNotIn(any(), any());
    }

    @Test
    @DisplayName("Debe cubrir la lambda de processOffersStream (Virtual Thread)")
    void shouldCoverProcessOffersStreamLambda() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("<xml></xml>".getBytes());
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
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
    @DisplayName("Debe cubrir el bloque catch de la lambda (Error de conexión)")
    void shouldCoverLambdaCatchBlock() throws Exception {
        URI unreachableUri = URI.create("http://localhost:1");
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        service.processOffersFromUrl(IngestionFormat.xml, unreachableUri, inventoryId, participantId,
                IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

        verify(reportSqlService, timeout(2000)).existsByPhysicalInventoryIdAndStatusNotIn(any(), any());

        Thread.sleep(500);
        verify(jobLauncher, never()).run(any(), any());
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
    @DisplayName("Debe promover ofertas exitosamente cuando el reporte no ha sido procesado")
    void shouldPromoteOffersSuccessfully() {
        UUID jobId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .processed(false)
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
    void shouldNotPromoteOffersWhenStatusIsNotCompleted() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .processed(true)
                .status(IngestionBatchStatus.STARTED)
                .dumpType(IngestionDumpType.REPLACEMENT)
                .inventoryId(inventoryId)
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, false);

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(
                any(), any(), any(), any(),
                any());

        verify(reportSqlService, never()).upsertIngestionReport(any());
    }

    @Test
    @DisplayName("No debe promover ofertas si el reporte ya marca 'processed' como true")
    void shouldNotPromoteIfAlreadyProcessed() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .processed(true)
                .status(IngestionBatchStatus.STARTED)
                .build();

        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, false);

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());
        verify(reportSqlService, never()).upsertIngestionReport(any());
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
        InputStream inputStream = new ByteArrayInputStream("<xml></xml>".getBytes());
        when(parserService.supports(any())).thenReturn(true);
        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        doThrow(new RuntimeException("Batch Exception")).when(jobLauncher).run(any(), any());

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());

        verify(reportSqlService, timeout(3000)).upsertIngestionReport(any());

        Thread.sleep(500);
    }

    @Test
    @DisplayName("Debe cubrir la rama del else en el catch de la lambda (execution == null)")
    void shouldCoverStreamLambdaCatchWithNullException() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());
        when(parserService.supports(any())).thenReturn(true);

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());
    }

    @Test
    @DisplayName("Debe cubrir la rama execution != null en el catch de processOffersStream")
    void shouldCoverStreamLambdaCatchWithExecutionNotNull() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());
        lenient().when(parserService.supports(any())).thenReturn(true);
        lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        JobExecution mockExecution = mock(JobExecution.class);
        lenient().when(mockExecution.getJobId()).thenReturn(123L);
        lenient().when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        lenient().when(jobLauncher.run(eq(offerIngestionJob), any(JobParameters.class)))
                .thenThrow(new RuntimeException("Forced failure"));

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
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
        when(parserService.supports(IngestionFormat.xml)).thenReturn(true);

        service.processOffersStream(
                IngestionFormat.xml,
                new ByteArrayInputStream("data".getBytes()),
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
        when(parserService.supports(any())).thenReturn(false);

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffersStream(
                        IngestionFormat.xml,
                        new ByteArrayInputStream("data".getBytes()),
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
        // GIVEN
        UUID requesterId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();

        List<IngestionBatchStatus> finalStatuses = List.of(
                IngestionBatchStatus.COMPLETED,
                IngestionBatchStatus.FAILED,
                IngestionBatchStatus.ABANDONED,
                IngestionBatchStatus.STOPPED
        );

        when(parserService.supports(IngestionFormat.xml)).thenReturn(true);

        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(eq(inventoryId), any()))
                .thenReturn(false);

        service.processOffersStream(
                IngestionFormat.xml,
                new ByteArrayInputStream("".getBytes()),
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

        when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(UUID.class), any()))
                .thenReturn(true);

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                service.processOffersStream(
                        IngestionFormat.xml,
                        new ByteArrayInputStream("".getBytes()),
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
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());

        lenient().when(parserService.supports(any())).thenReturn(true);
        lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        JobExecution mockExecution = mock(JobExecution.class);
        lenient().when(mockExecution.getJobId()).thenReturn(123L);
        lenient().when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        lenient().when(jobLauncher.run(eq(offerIngestionJob), any(JobParameters.class)))
                .thenAnswer(invocation -> {
                    throw new RuntimeException("Simulated Failure with Execution Object");
                });

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(3000)).run(any(), any());

        Thread.sleep(500);
    }

    @Test
    @DisplayName("Rama Catch: execution == null (Error directo en el Launcher)")
    void shouldCoverCatchWhenExecutionIsNull() throws Exception {

        InputStream inputStream = new ByteArrayInputStream("data".getBytes());
        lenient().when(parserService.supports(any())).thenReturn(true);

        doThrow(new RuntimeException("Direct Launcher Failure"))
                .when(jobLauncher).run(any(), any());

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
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
            lenient().when(parserService.supports(any())).thenReturn(true);
            lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

            service.processOffersFromUrl(IngestionFormat.xml, uri, inventoryId, participantId, IngestionDumpType.REPLACEMENT, null, "ext");

            Thread.sleep(1000);
            verify(jobLauncher, never()).run(any(), any());
        } finally { server.stop(0); }
    }

    @Test
    @DisplayName("Debe loguear error de batch cuando execution NO es nula")
    void shouldLogErrorWhenBatchFailsWithExecutionNotNull() throws Exception {
        InputStream stream = new ByteArrayInputStream("data".getBytes());

        lenient().when(parserService.supports(any())).thenReturn(true);
        lenient().when(reportSqlService.existsByPhysicalInventoryIdAndStatusNotIn(any(), any())).thenReturn(false);

        JobExecution mockExecution = mock(JobExecution.class);
        lenient().when(mockExecution.getJobId()).thenReturn(777L);
        lenient().when(mockExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        lenient().when(jobLauncher.run(any(), any())).thenAnswer(invocation -> {
            throw new RuntimeException("Simulated Failure");
        });

        service.processOffersStream(IngestionFormat.xml, stream, inventoryId, participantId, IngestionDumpType.REPLACEMENT, OffsetDateTime.now(), "ext-1");

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
    @DisplayName("Promote: Debería fallar si el reporte ya fue promovido")
    void validatePromotion_AlreadyPromoted() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .promoted(true) // Ya promovido
                .build();
        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, false);

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Promote: Debería fallar si el reporte no está completado")
    void validatePromotion_NotCompleted() {
        UUID jobId = UUID.randomUUID();
        IngestionReportDto report = IngestionReportDto.builder()
                .id(jobId)
                .promoted(false)
                .processed(true)
                .status(IngestionBatchStatus.FAILED) // No completado
                .build();
        when(reportSqlService.findIngestionReportById(jobId)).thenReturn(report);

        service.promoteDraftOffersToVehicleOffers(jobId, false);

        verify(offerNoSqlService, never()).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());
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

        // Forzamos error en la infraestructura
        doThrow(LCIngestionException.builder().techCause(LCTechCauseEnum.DATABASE).build())
                .when(offerNoSqlService).promoteDraftOffersToVehicleOffers(any(), any(), any(), any(), any());

        // Verificamos que al ser async=false, lanza la excepción
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
        InputStream inputStream = new ByteArrayInputStream("data".getBytes());
        lenient().when(parserService.supports(any())).thenReturn(true);

        // El launcher lanza la excepción directamente
        when(jobLauncher.run(any(), any())).thenThrow(new RuntimeException("Launcher Direct Failure"));

        service.processOffersStream(IngestionFormat.xml, inputStream, inventoryId, participantId,
                IngestionDumpType.INCREMENTAL, OffsetDateTime.now(), "ext-1");

        verify(jobLauncher, timeout(2000)).run(any(), any());
    }

}