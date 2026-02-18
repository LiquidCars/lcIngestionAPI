package net.liquidcars.ingestion.infra.input.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportActionMsg;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionReportDeleteInfraKafkaConsumer {

    private final IOfferInfraKafkaConsumerService offerInfraKafkaConsumerService;

    @KafkaListener(
            topics = "liquidcars.ingestion.event.report.delete-action.0",
            groupId = "liquidcars-ingestion-group"
    )
    public void consumeIngestionReportActionDelete(IngestionReportActionMsg message) {
        log.info("Received ingestion report job with id: {} for delete offers", message.getIngestionReportId());
        try {
            offerInfraKafkaConsumerService.processIngestionReportDeleteAction(UUID.fromString(message.getIngestionReportId()));
        } catch (Exception e) {
            log.error("Critical error processing report with id: {}. Triggering Kafka retry...", message.getIngestionReportId(), e);
            // We wrap and rethrow the exception.
            // By letting it propagate, Kafka's ErrorHandler will catch it.
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to process consumed job report with id: " + message.getIngestionReportId() + " for delete offers")
                    .cause(e)
                    .build();
        }
    }
}