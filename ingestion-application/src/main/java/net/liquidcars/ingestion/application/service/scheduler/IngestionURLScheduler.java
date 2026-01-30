package net.liquidcars.ingestion.application.service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(value = "ingestion.scheduler.enabled", havingValue = "true")
public class IngestionURLScheduler {

    private final IOfferIngestionProcessService ingestionService;

    @Value("${ingestion.scheduler.format:json}")
    private String format;

    @Value("${ingestion.scheduler.remoteUrl}")
    private String remoteUrl;


    @Scheduled(cron = "${ingestion.scheduler.cron}")
    public void scheduleIngestionFromUrl() {
        log.info("Scheduled task triggered: Starting ingestion from URL: {}", remoteUrl);
        try {
            URI uri = URI.create(remoteUrl);

            ingestionService.processOffersFromUrl(format, uri);

            log.info("Scheduled ingestion successfully launched for: {}", remoteUrl);
        } catch (Exception e) {
            log.error("Error launching scheduled ingestion for URL {}: {}", remoteUrl, e.getMessage());
        }
    }
}
