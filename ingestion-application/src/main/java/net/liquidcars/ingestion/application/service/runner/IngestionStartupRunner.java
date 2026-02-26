package net.liquidcars.ingestion.application.service.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
/* * The @ConditionalOnProperty ensures this bean is only created if the
 * property 'ingestion.startup.enabled' is set to 'true'.
 */
@ConditionalOnProperty(name = "ingestion.startUpRunner.enabled", havingValue = "true")
public class IngestionStartupRunner {

    public static final String EXTERNAL_PUBLICATION_ID = "STARTUP_AUTO_INGESTION";
    private final IOfferIngestionProcessService ingestionProcessService;

    private static final String STARTUP_FILE = "static/testFiles/MF_856b7d9a-cabd-4e86-aaca-b7a9641a9d0b_CC_2024_09_30_A.xml";
    private static final UUID TEST_INVENTORY_ID = UUID.fromString("856b7d9a-cabd-4e86-aaca-b7a9641a9d0b");
    private static final UUID TEST_PARTICIPANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Starting automatic ingestion for file: {}", STARTUP_FILE);

        try {
            // 1. Load resource from Classpath (Inside the JAR)
            ClassPathResource resource = new ClassPathResource(STARTUP_FILE);
            if (!resource.exists()) {
                log.warn("Startup file not found in classpath: {}", STARTUP_FILE);
                return;
            }

            InputStream inputStream = resource.getInputStream();

            // 2. Trigger ingestion process (this launches an internal Virtual Thread)
            ingestionProcessService.processOffersStream(
                    IngestionFormat.xml,
                    inputStream,
                    TEST_INVENTORY_ID,
                    TEST_PARTICIPANT_ID,
                    IngestionDumpType.REPLACEMENT,
                    OffsetDateTime.now(),
                    EXTERNAL_PUBLICATION_ID
            );

        } catch (Exception e) {
            log.error("Failed to execute startup ingestion", e);
        }
    }
}