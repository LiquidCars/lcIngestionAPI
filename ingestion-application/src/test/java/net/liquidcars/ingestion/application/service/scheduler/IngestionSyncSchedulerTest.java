package net.liquidcars.ingestion.application.service.scheduler;

import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class IngestionSyncSchedulerTest {

    @Mock
    private IOfferIngestionProcessService offerIngestionProcessService;

    @InjectMocks
    private IngestionSyncScheduler scheduler;

    @Test
    @DisplayName("Debe invocar syncPendingReports correctamente")
    void executeSync_Success() {
        // Act
        scheduler.executeSync();

        // Assert
        verify(offerIngestionProcessService, times(1)).syncPendingReports();
    }

    @Test
    @DisplayName("Debe propagar la excepción si el servicio falla")
    void executeSync_PropagatesException() {
        // Arrange
        doThrow(new RuntimeException("Error de sincronización"))
                .when(offerIngestionProcessService).syncPendingReports();

        // Act & Assert
        assertThatThrownBy(() -> scheduler.executeSync())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Error de sincronización");

        verify(offerIngestionProcessService, times(1)).syncPendingReports();
    }
}
