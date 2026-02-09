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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class OfferItemWriter implements ItemWriter<OfferDto>, StepExecutionListener {

    private final IOfferInfraKafkaProducerService kafkaProducer;
    private String jobIdentifier;
    private String jobStatus;

    @Override
    public void beforeStep(StepExecution stepExecution) {

        String ingestionId = stepExecution.getJobExecution()
                .getJobParameters()
                .getString("ingestionId");
        this.jobIdentifier = stepExecution.getJobExecution().getJobInstance().getJobName() + "-" + ingestionId;
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
        kafkaProducer.flushOffers();
    }
}