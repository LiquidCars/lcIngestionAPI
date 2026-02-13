package net.liquidcars.ingestion.application.service;

import net.liquidcars.ingestion.application.service.batch.IngestionSkipListener;
import net.liquidcars.ingestion.application.service.batch.JobCompletionNotificationListener;
import net.liquidcars.ingestion.application.service.batch.OfferItemWriter;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IBatchReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IReportInfraSQLService;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
    private JobRepository jobRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private IngestionSkipListener ingestionSkipListener;

    @Mock
    private JobCompletionNotificationListener jobCompletionListener;

    @Mock
    private Job offerIngestionJob;

    @Mock
    private OfferStreamItemReader offerReader;

    @Mock
    private IOfferInfraSQLService offerInfraSQLService;

    @Mock
    private IOfferInfraNoSQLService offerInfraNoSQLService;

    @Mock
    private IBatchReportInfraSQLService batchReportInfraSQLService;

    @Mock
    private IReportInfraSQLService reportInfraSQLService;

    @Captor
    private ArgumentCaptor<OfferDto> offerCaptor;

    @Captor
    private ArgumentCaptor<IngestionReportDto> reportCaptor;

    @Captor
    private ArgumentCaptor<IngestionBatchReportDto> batchReportCaptor;

    private UUID testInventoryId;
    private UUID testParticipantId;
    private String testExternalPublicationId;

    @BeforeEach
    void setUp() {
        testInventoryId = UUID.randomUUID();
        testParticipantId = UUID.randomUUID();
        testExternalPublicationId = "EXT-PUB-001";

        ReflectionTestUtils.setField(service, "chunkSize", 10);

        // Use lenient() for common setup mocks that may not be used in all tests
        lenient().when(mockParser.supports(any(IngestionFormat.class))).thenReturn(true);
        lenient().when(parsers.stream()).thenReturn(java.util.stream.Stream.of(mockParser));
    }

    // ==================== processOffers Tests ====================

    @Test
    void processOffers_ShouldProcessSuccessfully_WhenValidOffers() {
        IngestionPayloadDto ingestionPayloadDto = IngestionPayloadDto.builder().build();
        List<OfferDto> offers = createTestOffers(3);
        ingestionPayloadDto.setOffers(offers);

        when(reportInfraSQLService.existsByRequesterParticipantIdAndStatusNotIn(
                eq(testParticipantId), anyList())).thenReturn(false);

        service.processOffers(ingestionPayloadDto, testInventoryId, testParticipantId,
                IngestionDumpType.REPLACEMENT, testExternalPublicationId);

        verify(offerInfraKafkaProducerService, times(3)).sendOffer(offerCaptor.capture());
        verify(reportInfraSQLService, times(1)).upsertIngestionReport(reportCaptor.capture());
        verify(offerInfraKafkaProducerService, times(1)).sendIngestionJobReport(any(IngestionReportDto.class));

        IngestionReportDto savedReport = reportCaptor.getValue();
        assertEquals(3, savedReport.getReadCount());
        assertEquals(3, savedReport.getWriteCount());
        assertEquals(0, savedReport.getSkipCount());
        assertEquals(IngestionBatchStatus.STARTED, savedReport.getStatus());
        assertEquals(testInventoryId, savedReport.getInventoryId());
        assertEquals(testParticipantId, savedReport.getRequesterParticipantId());
    }

    @Test
    void processOffers_ShouldThrowException_WhenOffersListIsNull() {
        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffers(null, testInventoryId, testParticipantId,
                        IngestionDumpType.REPLACEMENT, testExternalPublicationId)
        );

        assertEquals("The offers list is empty or null", ex.getMessage());
        assertEquals(LCTechCauseEnum.INVALID_REQUEST, ex.getTechCause());
    }

    @Test
    void processOffers_ShouldThrowException_WhenOffersListIsEmpty() {
        IngestionPayloadDto ingestionPayloadDto = IngestionPayloadDto.builder().build();
        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffers(ingestionPayloadDto, testInventoryId, testParticipantId,
                        IngestionDumpType.REPLACEMENT, testExternalPublicationId)
        );

        assertEquals("The offers list is empty or null", ex.getMessage());
    }

    @Test
    void processOffers_ShouldThrowException_WhenParticipantHasActiveProcess() {
        when(reportInfraSQLService.existsByRequesterParticipantIdAndStatusNotIn(
                eq(testParticipantId), anyList())).thenReturn(true);

        IngestionPayloadDto ingestionPayloadDto = IngestionPayloadDto.builder().build();
        List<OfferDto> offers = createTestOffers(2);
        ingestionPayloadDto.setOffers(offers);

        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffers(ingestionPayloadDto, testInventoryId, testParticipantId,
                        IngestionDumpType.REPLACEMENT, testExternalPublicationId)
        );

        assertTrue(ex.getMessage().contains("already has an active ingestion process"));
        verify(offerInfraKafkaProducerService, never()).sendOffer(any());
    }

    @Test
    void processOffers_ShouldSetIngestionReportIdOnEachOffer() {
        IngestionPayloadDto ingestionPayloadDto = IngestionPayloadDto.builder().build();
        List<OfferDto> offers = createTestOffers(2);
        ingestionPayloadDto.setOffers(offers);

        when(reportInfraSQLService.existsByRequesterParticipantIdAndStatusNotIn(
                eq(testParticipantId), anyList())).thenReturn(false);

        service.processOffers(ingestionPayloadDto, testInventoryId, testParticipantId,
                IngestionDumpType.REPLACEMENT, testExternalPublicationId);

        verify(offerInfraKafkaProducerService, times(2)).sendOffer(offerCaptor.capture());

        List<OfferDto> capturedOffers = offerCaptor.getAllValues();
        assertNotNull(capturedOffers.get(0).getIngestionReportId());
        assertNotNull(capturedOffers.get(1).getIngestionReportId());
        assertEquals(capturedOffers.get(0).getIngestionReportId(),
                capturedOffers.get(1).getIngestionReportId());
    }

    // ==================== processOffersFromUrl Tests ====================

    @Test
    void processOffersFromUrl_ShouldThrowException_WhenUrlIsNull() {
        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffersFromUrl(IngestionFormat.json, null, testInventoryId,
                        testParticipantId, IngestionDumpType.REPLACEMENT, OffsetDateTime.now(),
                        testExternalPublicationId)
        );

        assertTrue(ex.getMessage().contains("malformed or null"));
    }

    @Test
    void processOffersFromUrl_ShouldThrowException_WhenFormatNotSupported() {
        when(parsers.stream()).thenReturn(java.util.stream.Stream.empty());

        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffersFromUrl(IngestionFormat.xml, URI.create("http://example.com/data.xml"),
                        testInventoryId, testParticipantId, IngestionDumpType.REPLACEMENT,
                        OffsetDateTime.now(), testExternalPublicationId)
        );

        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void processOffersFromUrl_ShouldThrowException_WhenParticipantHasActiveProcess() {
        when(reportInfraSQLService.existsByRequesterParticipantIdAndStatusNotIn(
                eq(testParticipantId), anyList())).thenReturn(true);

        LCIngestionException ex = assertThrows(LCIngestionException.class, () ->
                service.processOffersFromUrl(IngestionFormat.json, URI.create("http://example.com/data.json"),
                        testInventoryId, testParticipantId, IngestionDumpType.REPLACEMENT,
                        OffsetDateTime.now(), testExternalPublicationId)
        );

        assertTrue(ex.getMessage().contains("already has an active ingestion process"));
    }

    // ==================== processOffersStream Tests ====================

    @Test
    void processOffersStream_ShouldStartBatchJob() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        JobExecution mockExecution = mock(JobExecution.class);

        when(reportInfraSQLService.existsByRequesterParticipantIdAndStatusNotIn(
                eq(testParticipantId), anyList())).thenReturn(false);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(mockExecution);

        service.processOffersStream(IngestionFormat.json, inputStream, testInventoryId,
                testParticipantId, IngestionDumpType.REPLACEMENT, OffsetDateTime.now(),
                testExternalPublicationId);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(offerReader, atLeastOnce()).start(eq(mockParser), any(InputStream.class));
            verify(jobLauncher, atLeastOnce()).run(eq(offerIngestionJob), any(JobParameters.class));
            verify(reportInfraSQLService, atLeastOnce()).upsertIngestionReport(any(IngestionReportDto.class));
            verify(offerInfraKafkaProducerService, atLeastOnce()).sendIngestionJobReport(any(IngestionReportDto.class));
        });
    }

    // ==================== processIngestionBatchReport Tests ====================

    @Test
    void processIngestionBatchReport_ShouldMarkAsProcessed_WhenCompletedAndCountsMatch() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto batchReport = createBatchReport(jobId, IngestionBatchStatus.COMPLETED, 10, 10);
        IngestionReportDto ingestionReport = createIngestionReport(jobId);

        when(offerInfraNoSQLService.countOffersFromJobId(jobId)).thenReturn(10L);
        when(reportInfraSQLService.findIngestionReportByBatchJobId(jobId)).thenReturn(ingestionReport);

        service.processIngestionBatchReport(batchReport);

        verify(batchReportInfraSQLService, times(2)).upsertIngestionBatchReport(batchReportCaptor.capture());
        verify(reportInfraSQLService, times(1)).upsertIngestionReport(any(IngestionReportDto.class));
        verify(offerInfraKafkaProducerService, times(1)).sendIngestionJobReport(any(IngestionReportDto.class));

        assertTrue(batchReportCaptor.getValue().isProcessed());
    }

    @Test
    void processIngestionBatchReport_ShouldNotMarkAsProcessed_WhenCompletedButCountsMismatch() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto batchReport = createBatchReport(jobId, IngestionBatchStatus.COMPLETED, 10, 10);

        when(offerInfraNoSQLService.countOffersFromJobId(jobId)).thenReturn(7L);

        service.processIngestionBatchReport(batchReport);

        verify(batchReportInfraSQLService, times(1)).upsertIngestionBatchReport(any(IngestionBatchReportDto.class));
        verify(reportInfraSQLService, never()).upsertIngestionReport(any(IngestionReportDto.class));
        verify(offerInfraKafkaProducerService, never()).sendIngestionJobReport(any(IngestionReportDto.class));
    }

    @Test
    void processIngestionBatchReport_ShouldMarkAsProcessed_WhenFailed() {
        UUID jobId = UUID.randomUUID();
        IngestionBatchReportDto batchReport = createBatchReport(jobId, IngestionBatchStatus.FAILED, 10, 5);
        IngestionReportDto ingestionReport = createIngestionReport(jobId);

        when(offerInfraNoSQLService.countOffersFromJobId(jobId)).thenReturn(5L);
        when(reportInfraSQLService.findIngestionReportByBatchJobId(jobId)).thenReturn(ingestionReport);

        service.processIngestionBatchReport(batchReport);

        verify(batchReportInfraSQLService, times(2)).upsertIngestionBatchReport(batchReportCaptor.capture());
        assertTrue(batchReportCaptor.getValue().isProcessed());
    }

    // ==================== processIngestionReport Tests ====================

    @Test
    void processIngestionReport_ShouldMarkAsProcessed_WhenCountsMatch() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto report = createIngestionReportWithId(reportId, 5);

        when(offerInfraNoSQLService.countOffersFromReportId(reportId)).thenReturn(5L);

        service.processIngestionReport(report);

        verify(reportInfraSQLService, times(1)).upsertIngestionReport(reportCaptor.capture());
        verify(offerInfraKafkaProducerService, times(1)).sendIngestionJobReport(any(IngestionReportDto.class));

        IngestionReportDto savedReport = reportCaptor.getValue();
        assertEquals(IngestionBatchStatus.COMPLETED, savedReport.getStatus());
    }

    @Test
    void processIngestionReport_ShouldNotMarkAsProcessed_WhenCountsMismatch() {
        UUID reportId = UUID.randomUUID();
        IngestionReportDto report = createIngestionReportWithId(reportId, 10);

        when(offerInfraNoSQLService.countOffersFromReportId(reportId)).thenReturn(7L);

        service.processIngestionReport(report);

        verify(reportInfraSQLService, never()).upsertIngestionReport(any());
        verify(offerInfraKafkaProducerService, never()).sendIngestionJobReport(any());
    }

    // ==================== Sync Tests ====================

    @Test
    void syncPendingBatchReports_ShouldProcessAllPendingReports() {
        List<IngestionBatchReportDto> pendingReports = List.of(
                createBatchReport(UUID.randomUUID(), IngestionBatchStatus.COMPLETED, 5, 5),
                createBatchReport(UUID.randomUUID(), IngestionBatchStatus.COMPLETED, 3, 3)
        );

        when(batchReportInfraSQLService.getBatchPendingReports()).thenReturn(pendingReports);
        when(offerInfraNoSQLService.countOffersFromJobId(any())).thenReturn(5L, 3L);
        when(reportInfraSQLService.findIngestionReportByBatchJobId(any()))
                .thenReturn(createIngestionReport(UUID.randomUUID()));

        service.syncPendingBatchReports();

        verify(batchReportInfraSQLService, times(2)).upsertIngestionBatchReport(any());
    }

    @Test
    void syncPendingBatchReports_ShouldDoNothing_WhenNoPendingReports() {
        when(batchReportInfraSQLService.getBatchPendingReports()).thenReturn(List.of());

        service.syncPendingBatchReports();

        verify(offerInfraNoSQLService, never()).countOffersFromJobId(any());
    }

    @Test
    void syncPendingReports_ShouldProcessAllPendingReports() {
        List<IngestionReportDto> pendingReports = List.of(
                createIngestionReportWithId(UUID.randomUUID(), 5),
                createIngestionReportWithId(UUID.randomUUID(), 3)
        );

        when(reportInfraSQLService.getPendingReports()).thenReturn(pendingReports);
        when(offerInfraNoSQLService.countOffersFromReportId(any())).thenReturn(5L, 3L);

        service.syncPendingReports();

        verify(reportInfraSQLService, times(2)).upsertIngestionReport(any());
    }

    // ==================== Factory Methods using TestDataFactory ====================

    private List<OfferDto> createTestOffers(int count) {
        return TestDataFactory.createOfferDtoList(count);
    }

    private OfferDto createOfferDto() {
        return TestDataFactory.createOfferDto();
    }

    private IngestionBatchReportDto createBatchReport(UUID jobId, IngestionBatchStatus status,
                                                      int readCount, int writeCount) {
        return TestDataFactory.createBatchReportWithCounts(
                jobId, status, readCount, writeCount, 0L
        );
    }

    private IngestionReportDto createIngestionReport(UUID batchJobId) {
        return TestDataFactory.createIngestionReportWithBatchJobId(batchJobId);
    }

    private IngestionReportDto createIngestionReportWithId(UUID reportId, int writeCount) {
        return TestDataFactory.createIngestionReportWithDetails(
                reportId, testParticipantId, testInventoryId, writeCount
        );
    }
}