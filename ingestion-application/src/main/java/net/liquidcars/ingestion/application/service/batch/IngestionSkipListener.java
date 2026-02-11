package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionSkipListener implements SkipListener<Object, Object> {

    private final JobFailedIdsCollector failedIdsCollector;

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof LCIngestionParserException ex) {
            log.warn(">> [SKIP_READ] Record skipped.  ID: {} | Cause: {} | Technical Code: {} | Message: {}",
                    ex.getFailedIdentifier(), ex.getTechCause(), ex.getErrorCode(), ex.getMessage());

            if (ex.getFailedIdentifier() != null) {
                failedIdsCollector.addId(ex.getFailedIdentifier());
            }
        } else if (t instanceof LCIngestionException ex) {
            log.warn(">> [SKIP_READ] Record skipped. Cause: {} | Technical Code: {} | Message: {}",
                    ex.getTechCause(), ex.getErrorCode(), ex.getMessage());
        } else {
            log.error(">> [SKIP_READ] Unexpected unhandled error during reading: ", t);
        }

    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        if (item instanceof OfferDto offer) {
            log.error(">> [SKIP_WRITE] Failed to send offer: {} | Error: {}",
                    offer.getId(), t.getMessage());
            failedIdsCollector.addId(offer.getId());
        } else {
            log.error(">> [SKIP_WRITE] Failed to send unknown item type: {}. Error: {}",
                    item, t.getMessage());
        }
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        log.warn(">> [SKIP_PROCESS] Processing failed for item: {}. Reason: {}", item, t.getMessage());
    }
}