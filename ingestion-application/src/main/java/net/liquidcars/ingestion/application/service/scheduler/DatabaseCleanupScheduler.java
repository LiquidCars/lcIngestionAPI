package net.liquidcars.ingestion.application.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ingestion.purgeScheduler", name = "enabled", havingValue = "true")
public class DatabaseCleanupScheduler {

    private final IOfferInfraNoSQLService noSQLService;
    private final IOfferInfraSQLService sqlService;

    @Value("${ingestion.purgeScheduler.days:7}")
    private int daysToKeep;

    /**
     * Scheduled task to clean up obsolete offers.
     * The cron expression is pulled directly from application.yml
     */
    @Scheduled(cron = "${ingestion.purgeScheduler.cron}")
    public void scheduleOfferPurge() {
        log.info("Starting scheduled purge. Criteria: status != COMPLETED, older than {} days", daysToKeep);

        try {
            noSQLService.purgeObsoleteOffers(daysToKeep);
            log.info("Purge task finished successfully.");
        } catch (Exception e) {
            log.error("Error during scheduled purge execution", e);
        }
    }
}
