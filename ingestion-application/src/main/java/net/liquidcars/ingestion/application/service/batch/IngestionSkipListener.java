package net.liquidcars.ingestion.application.service.batch;

import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class IngestionSkipListener implements SkipListener<Object, Object> {

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof LCIngestionException ex) {
            log.warn(">> [SKIP_READ] Record skipped. Cause: {} | Technical Code: {} | Message: {}",
                    ex.getTechCause(), ex.getErrorCode(), ex.getMessage());
        } else {
            log.error(">> [SKIP_READ] Unexpected unhandled error during reading: ", t);
        }
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        log.error(">> [SKIP_WRITE] Failed to send item: {}. Error: {}", item, t.getMessage());
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn(">> [SKIP_PROCESS] Processing failed for item: {}. Reason: {}", item, t.getMessage());
    }
}