package net.liquidcars.ingestion.application.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ingestion.syncScheduler", name = "enabled", havingValue = "true")
public class IngestionSyncScheduler {

    private final IOfferIngestionProcessService offerIngestionProcessService;

    /**
     * Scheduled task to sync offers with completed jobs.
     * The cron expression is pulled directly from application.yml
     */
    @Scheduled(cron = "${ingestion.syncScheduler.cron}")
    public void executeSync() {
        log.info("Starting synchronized pending reports process...");
        offerIngestionProcessService.syncPendingReports();
        log.info("Finished synchronized process.");
    }
}