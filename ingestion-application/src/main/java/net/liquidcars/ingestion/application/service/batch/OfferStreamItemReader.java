package net.liquidcars.ingestion.application.service.batch;

import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class OfferStreamItemReader implements ItemReader<OfferDto>, StepExecutionListener {

    private BlockingQueue<ParsingResult> queue;
    private volatile boolean isParsingFinished = false;
    private volatile Throwable fatalError = null;
    private JobDeleteExternalIdsCollector deleteExternalIdsCollector;
    @Value("${ingestion.batch.queue-size:5000}")
    private int queueCapacity;

    public OfferStreamItemReader(@Value("${ingestion.batch.queue-size:5000}") int queueCapacity) {
        this.queueCapacity = queueCapacity;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
    }

    @Override
    public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
        stepExecution.getJobExecution().getExecutionContext().put("deleteExternalIdsCollector", this.deleteExternalIdsCollector);
    }

    public void start(IOfferParserService parser, InputStream is, JobDeleteExternalIdsCollector deleteExternalIdsCollector) {
        this.deleteExternalIdsCollector = deleteExternalIdsCollector;
        this.queue = new LinkedBlockingQueue<>(queueCapacity);
        this.isParsingFinished = false;
        this.fatalError = null;

        Thread.ofVirtual().start(() -> {
            try (is) {
                // We pass a lambda that wraps the success in a ParsingResult
                parser.parseAndProcess(is, offer -> {
                    try {
                        queue.put(ParsingResult.success(offer));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new LCIngestionException(LCTechCauseEnum.INTERNAL_ERROR, "Interrupted error", e);
                    }
                }, deleteExternalIdsCollector);
            } catch (LCIngestionException e) {
                log.debug("Parser thread caught a record error already queued");
            } catch (Exception e) {
                this.fatalError = e;
            } finally {
                this.isParsingFinished = true;
            }
        });
    }

    @Override
    public OfferDto read() throws Exception {
        if (fatalError != null) throw new RuntimeException(fatalError);

        while (true) {
            ParsingResult result = queue.poll(200, TimeUnit.MILLISECONDS);

            if (result != null) {
                if (result.isError()) {
                    throw result.error(); // Spring Batch captures it, SkipListener takes over, and the Job CONTINUES
                }
                return result.offer(); // Register OK
            }

            if (isParsingFinished && queue.isEmpty()) return null;
        }
    }

    public void addErrorToQueue(LCIngestionException e) {
        try {
            queue.put(ParsingResult.failure(e));
        } catch (InterruptedException ie) {
            log.error("Interrupted error", e);
            Thread.currentThread().interrupt();
        }
    }

    public record ParsingResult(OfferDto offer, LCIngestionException error) {
        public static ParsingResult success(OfferDto dto) {
            return new ParsingResult(dto, null);
        }
        public static ParsingResult failure(LCIngestionException e) {
            return new ParsingResult(null, e);
        }
        public boolean isError() {
            return error != null;
        }
    }
}