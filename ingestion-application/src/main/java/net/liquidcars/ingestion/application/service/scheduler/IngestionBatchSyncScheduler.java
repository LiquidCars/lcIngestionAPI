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
@ConditionalOnProperty(prefix = "ingestion.batchSyncScheduler", name = "enabled", havingValue = "true")
public class IngestionBatchSyncScheduler {

    private final IOfferIngestionProcessService offerIngestionProcessService;

    /**
     * Scheduled task to sync offers with completed jobs.
     * The cron expression is pulled directly from application.yml
     */
    @Scheduled(cron = "${ingestion.batchSyncScheduler.cron}")
    public void executeSync() {
        log.info("Starting synchronized pending batch reports process...");
        offerIngestionProcessService.syncPendingBatchReports();
        log.info("Finished synchronized batch process.");
    }
}