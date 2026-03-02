package net.liquidcars.ingestion.application.service.scheduler;

import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DatabaseCleanupSchedulerTest {

    @Mock
    private IOfferInfraNoSQLService noSQLService;

    @Mock
    private IOfferInfraSQLService sqlService;

    @InjectMocks
    private DatabaseCleanupScheduler scheduler;

    private final int DAYS_TO_KEEP = 15;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "daysToKeep", DAYS_TO_KEEP);
    }

    @Test
    void scheduleOfferPurge_ShouldInvokePurgeWithCorrectDays() {
        scheduler.scheduleOfferPurge();

        verify(noSQLService, times(1)).purgeObsoleteOffers(DAYS_TO_KEEP);
    }

    @Test
    void scheduleOfferPurge_ShouldHandleException_WhenNoSQLServiceFails() {
        doThrow(new RuntimeException("Database connection failed"))
                .when(noSQLService).purgeObsoleteOffers(anyInt());

        scheduler.scheduleOfferPurge();

        verify(noSQLService, times(1)).purgeObsoleteOffers(DAYS_TO_KEEP);
    }
}
