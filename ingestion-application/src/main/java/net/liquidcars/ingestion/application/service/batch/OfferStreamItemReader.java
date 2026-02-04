package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.batch.item.ItemReader;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class OfferStreamItemReader implements ItemReader<OfferDto> {

    private final BlockingQueue<OfferDto> queue = new LinkedBlockingQueue<>(500);
    private boolean isParsingFinished = false;
    private Throwable error = null;

    public OfferStreamItemReader(IOfferParserService parser, InputStream is) {
        Thread.ofVirtual().start(() -> {
            try (is) {
                parser.parseAndProcess(is, queue::add);
            } catch (Exception e) {
                this.error = e;
            } finally {
                this.isParsingFinished = true;
            }
        });
    }

    @Override
    public OfferDto read() {
        if (error != null) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.CONVERSION_ERROR)
                    .message("Error during stream parsing: " + error.getMessage())
                    .cause(error)
                    .build();
        }
        try {
            while (queue.isEmpty()) {
                if (isParsingFinished) return null;
                TimeUnit.MILLISECONDS.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INTERNAL_ERROR)
                    .message("Reader thread interrupted")
                    .cause(e)
                    .build();
        }
        return queue.poll();
    }

}