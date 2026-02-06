package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfferItemWriter implements ItemWriter<OfferDto>, StepExecutionListener {

    private final IOfferInfraKafkaProducerService kafkaProducer;
    private String jobIdentifier;
    private String jobStatus;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        this.jobIdentifier = stepExecution.getJobExecution().getJobInstance().getJobName() + "-" + stepExecution.getJobExecutionId();
        this.jobStatus = stepExecution.getJobExecution().getStatus().name();

        log.info("Writer configured. Job: {} | Status: {}", jobIdentifier, jobStatus);
    }

    @Override
    public void write(Chunk<? extends OfferDto> chunk) {
        for (OfferDto offer : chunk) {
            offer.setJobIdentifier(this.jobIdentifier);
            offer.setBatchStatus(this.jobStatus);
            kafkaProducer.sendOffer(offer);
        }
    }
}