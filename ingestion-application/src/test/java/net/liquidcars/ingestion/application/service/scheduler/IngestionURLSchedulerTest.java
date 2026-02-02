package net.liquidcars.ingestion.application.service.scheduler;

import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionURLSchedulerTest {

    @Mock
    private IOfferIngestionProcessService ingestionService;

    @InjectMocks
    private IngestionURLScheduler scheduler;

    private final String testUrl = "https://api.test.com/v1/offers";
    private final String testFormat = "json";

    @BeforeEach
    void setUp() {
        // Inyectamos manualmente los valores de @Value usando ReflectionTestUtils
        ReflectionTestUtils.setField(scheduler, "remoteUrl", testUrl);
        ReflectionTestUtils.setField(scheduler, "format", testFormat);
    }

    @Test
    void scheduleIngestionFromUrl_ShouldCallService_WhenTriggered() {
        scheduler.scheduleIngestionFromUrl();

        verify(ingestionService, times(1))
                .processOffersFromUrl(eq(testFormat), eq(URI.create(testUrl)));
    }

    @Test
    void scheduleIngestionFromUrl_ShouldHandleException_WhenUriIsInvalid() {
        ReflectionTestUtils.setField(scheduler, "remoteUrl", "esto no es una url");

        scheduler.scheduleIngestionFromUrl();

        verify(ingestionService, never()).processOffersFromUrl(any(), any());
    }

    @Test
    void scheduleIngestionFromUrl_ShouldCatchServiceExceptions() {
        doThrow(new RuntimeException("Service Error"))
                .when(ingestionService).processOffersFromUrl(anyString(), any(URI.class));

        scheduler.scheduleIngestionFromUrl();

        verify(ingestionService).processOffersFromUrl(anyString(), any(URI.class));
    }
}