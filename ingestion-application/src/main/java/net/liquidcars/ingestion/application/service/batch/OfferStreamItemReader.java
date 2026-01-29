package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
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
    public OfferDto read() throws Exception {
        if (error != null) throw new RuntimeException("Error en el parser", error);

        while (queue.isEmpty()) {
            if (isParsingFinished) return null;
            TimeUnit.MILLISECONDS.sleep(10);
        }
        return queue.poll();
    }

}