package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class OfferItemWriter implements ItemWriter<OfferDto>, StepExecutionListener {

    private final IOfferInfraKafkaProducerService kafkaProducer;
    private StepExecution stepExecution;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        log.info("Writer configured for job: {}", getJobIdentifier());
    }

    @Override
    public void write(Chunk<? extends OfferDto> chunk) {
        for (OfferDto offer : chunk) {
            offer.setJobIdentifier(this.getJobIdentifier());
            offer.setBatchStatus(this.getJobStatus());
            kafkaProducer.sendOffer(offer);
        }
    }

    private String getJobIdentifier() {
        if (stepExecution == null) {
            log.warn("StepExecution not yet initialized!");
            return null;
        }
        String ingestionId = stepExecution.getJobExecution()
                .getJobParameters()
                .getString("ingestionId");
        return stepExecution.getJobExecution().getJobInstance().getJobName() + "-" + ingestionId;
    }

    private String getJobStatus() {
        return stepExecution != null ?
                stepExecution.getJobExecution().getStatus().name() :
                null;
    }
}