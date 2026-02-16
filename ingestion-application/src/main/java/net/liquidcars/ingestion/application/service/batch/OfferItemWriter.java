package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
            setExternalIdsToOffer(offer);
            kafkaProducer.sendOffer(offer);
        }
    }

    private void setExternalIdsToOffer(OfferDto offer) {
        offer.setJobIdentifier(this.getJobIdentifier());
        offer.setIngestionReportId(this.getIngestionReportId());
        offer.setParticipantId(this.getRequesterParticipantId());
        offer.setInventoryId(this.getInventoryId());
    }

    private UUID getJobIdentifier() {
        if (stepExecution == null) {
            log.warn("StepExecution not yet initialized!");
            return null;
        }
        String ingestionIdParam = stepExecution.getJobExecution()
                .getJobParameters()
                .getString("ingestionId");
        if (ingestionIdParam == null) {
            log.warn("Skipping ingestion report: ingestionId is null");
            return null;
        }
        return UUID.fromString(ingestionIdParam);
    }

    private UUID getIngestionReportId() {
        if (stepExecution == null) {
            log.warn("StepExecution not yet initialized!");
            return null;
        }
        String ingestionReportIdParam = stepExecution.getJobExecution()
                .getJobParameters()
                .getString("ingestionReportId");
        if (ingestionReportIdParam == null) {
            log.warn("Skipping ingestion report: ingestionReportId is null");
            return null;
        }
        return UUID.fromString(ingestionReportIdParam);
    }

    private UUID getRequesterParticipantId() {
        if (stepExecution == null) {
            log.warn("StepExecution not yet initialized!");
            return null;
        }
        String requesterParticipantIdParam = stepExecution.getJobExecution()
                .getJobParameters()
                .getString("requesterParticipantId");
        if (requesterParticipantIdParam == null) {
            log.warn("Skipping ingestion report: requesterParticipantId is null");
            return null;
        }
        return UUID.fromString(requesterParticipantIdParam);
    }

    private UUID getInventoryId() {
        if (stepExecution == null) {
            log.warn("StepExecution not yet initialized!");
            return null;
        }
        String inventoryIdParam = stepExecution.getJobExecution()
                .getJobParameters()
                .getString("inventoryId");
        if (inventoryIdParam == null) {
            log.warn("Skipping ingestion report: inventoryId is null");
            return null;
        }
        return UUID.fromString(inventoryIdParam);
    }

}