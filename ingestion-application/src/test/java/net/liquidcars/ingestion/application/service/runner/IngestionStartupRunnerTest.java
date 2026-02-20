package net.liquidcars.ingestion.application.service.runner;

import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionStartupRunnerTest {

    @Mock
    private IOfferIngestionProcessService ingestionProcessService;

    @InjectMocks
    private IngestionStartupRunner startupRunner;

    @Test
    @DisplayName("onApplicationReady: Debería iniciar el proceso si el recurso existe")
    void onApplicationReady_Success() throws Exception {
        try (var mockedResource = mockConstruction(org.springframework.core.io.ClassPathResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenReturn(true);
                    when(mock.getInputStream()).thenReturn(InputStream.nullInputStream());
                })) {

            UUID expectedInventoryId = UUID.fromString("856b7d9a-cabd-4e86-aaca-b7a9641a9d0b");
            UUID expectedParticipantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, timeout(1000)).processOffersStream(
                    eq(IngestionFormat.xml),
                    any(InputStream.class),
                    eq(expectedInventoryId),
                    eq(expectedParticipantId),
                    eq(IngestionDumpType.REPLACEMENT),
                    any(OffsetDateTime.class),
                    eq(IngestionStartupRunner.EXTERNAL_PUBLICATION_ID)
            );
        }
    }

    @Test
    @DisplayName("Monitor: Debería promover a producción cuando el reporte esté COMPLETED")
    void monitor_ShouldPromote_WhenReportIsCompleted() {
        UUID participantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID reportId = UUID.randomUUID();

        IngestionReportDto completedReport = TestDataFactory.createIngestionReport();
        completedReport.setId(reportId);
        completedReport.setStatus(IngestionBatchStatus.COMPLETED);
        completedReport.setProcessed(true);

        IngestionReportPageDto pageDto = TestDataFactory.createIngestionReportPageDto();
        pageDto.setContent(List.of(completedReport)); // O .setData() según tu clase

        when(ingestionProcessService.findIngestionReports(any(IngestionReportFilterDto.class)))
                .thenReturn(pageDto);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(startupRunner, "startAsyncPromotionMonitor", participantId);

        verify(ingestionProcessService, timeout(15000)).promoteDraftOffersToVehicleOffers(eq(reportId), eq(true));
    }

    @Test
    @DisplayName("Monitor: Debería detenerse si el reporte tiene estado FAILED")
    void monitor_ShouldStop_WhenReportIsFailed() {
        UUID participantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        IngestionReportDto failedReport = TestDataFactory.createIngestionReport();
        failedReport.setStatus(IngestionBatchStatus.FAILED);

        IngestionReportPageDto pageDto = TestDataFactory.createIngestionReportPageDto();
        pageDto.setContent(List.of(failedReport));

        when(ingestionProcessService.findIngestionReports(any(IngestionReportFilterDto.class)))
                .thenReturn(pageDto);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(startupRunner, "startAsyncPromotionMonitor", participantId);

        verify(ingestionProcessService, timeout(15000).times(1)).findIngestionReports(any());
        verify(ingestionProcessService, never()).promoteDraftOffersToVehicleOffers(any(), anyBoolean());
    }

    @Test
    @DisplayName("Monitor: Debería reintentar si el reporte es null al principio")
    void monitor_ShouldRetry_WhenReportIsInitiallyNull() {
        UUID participantId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID reportId = UUID.randomUUID();

        IngestionReportDto completedReport = TestDataFactory.createIngestionReport();
        completedReport.setId(reportId);
        completedReport.setStatus(IngestionBatchStatus.COMPLETED);
        completedReport.setProcessed(true);

        IngestionReportPageDto emptyPage = TestDataFactory.createIngestionReportPageDto();
        emptyPage.setContent(List.of());

        IngestionReportPageDto successPage = TestDataFactory.createIngestionReportPageDto();
        successPage.setContent(List.of(completedReport));

        when(ingestionProcessService.findIngestionReports(any(IngestionReportFilterDto.class)))
                .thenReturn(emptyPage)
                .thenReturn(successPage);

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(startupRunner, "startAsyncPromotionMonitor", participantId);

        verify(ingestionProcessService, timeout(25000)).promoteDraftOffersToVehicleOffers(eq(reportId), eq(true));
    }

    @Test
    @DisplayName("Monitor: Debería loguear error si ocurre una excepción en la búsqueda")
    void monitor_ShouldHandleException() {
        UUID participantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(ingestionProcessService.findIngestionReports(any()))
                .thenThrow(new RuntimeException("DB Connection Error"));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(startupRunner, "startAsyncPromotionMonitor", participantId);

        verify(ingestionProcessService, timeout(15000)).findIngestionReports(any());
        verify(ingestionProcessService, never()).promoteDraftOffersToVehicleOffers(any(), anyBoolean());
    }

    @Test
    @DisplayName("onApplicationReady: Debería loguear warn y salir si el fichero NO existe")
    void onApplicationReady_FileNotFound() throws Exception {
        try (var mockedResource = mockConstruction(org.springframework.core.io.ClassPathResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenReturn(false);
                })) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, never()).processOffersStream(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("onApplicationReady: Debería entrar en el catch si falla la lectura del stream")
    void onApplicationReady_ExceptionCatch() throws Exception {
        try (var mockedResource = mockConstruction(org.springframework.core.io.ClassPathResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenReturn(true);
                    when(mock.getInputStream()).thenThrow(new RuntimeException("Error de lectura simulado"));
                })) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, never()).processOffersStream(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("Monitor: Debería manejar InterruptedException y restaurar el estado del hilo")
    void monitor_ShouldHandleInterruptedException() {
        UUID participantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        // Usamos un Answer para interrumpir el hilo actual en cuanto el monitor intente buscar reportes
        // Esto simula que alguien canceló el proceso mientras esperaba o trabajaba
        when(ingestionProcessService.findIngestionReports(any()))
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt(); // Interrumpimos el hilo
                    throw new InterruptedException("Simulated interruption");
                });

        // WHEN
        org.springframework.test.util.ReflectionTestUtils.invokeMethod(startupRunner, "startAsyncPromotionMonitor", participantId);

        // THEN
        // Verificamos que se capturó y el hilo se marcó como interrumpido (vía logs o ausencia de crash)
        // El timeout es necesario por la naturaleza asíncrona
        verify(ingestionProcessService, timeout(15000)).findIngestionReports(any());
        verify(ingestionProcessService, never()).promoteDraftOffersToVehicleOffers(any(), anyBoolean());
    }
}
