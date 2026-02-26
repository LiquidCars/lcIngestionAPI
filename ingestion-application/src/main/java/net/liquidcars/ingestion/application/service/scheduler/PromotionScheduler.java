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
@ConditionalOnProperty(prefix = "ingestion.promotionScheduler", name = "enabled", havingValue = "true")
public class PromotionScheduler {

    private final IOfferIngestionProcessService offerIngestionProcessService;

    /**
     * Scheduled task to promote offers with past or present publication dates.
     * The cron expression is pulled directly from application.yml
     */
    @Scheduled(cron = "${ingestion.promotionScheduler.cron}")
    public void executeDeferredPromotions() {
        log.info("Starting promotion of pending reports process...");
        offerIngestionProcessService.executeDeferredPromotions();
        log.info("Finished promotion process.");
    }
}
