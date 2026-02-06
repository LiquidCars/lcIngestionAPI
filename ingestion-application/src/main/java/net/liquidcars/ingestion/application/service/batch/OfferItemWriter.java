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

        var jobExecution = stepExecution.getJobExecution();

        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        Long executionId = stepExecution.getJobExecutionId();
        this.jobIdentifier = jobName + "_" + executionId;
        this.jobStatus = jobExecution.getStatus().name();

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