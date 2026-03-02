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
public class IngestionReportPromoteInfraKafkaConsumer {

    private final IOfferInfraKafkaConsumerService offerInfraKafkaConsumerService;

    @KafkaListener(
            topics = "liquidcars.ingestion.event.report.promote-action.0",
            groupId = "liquidcars-ingestion-group"
    )
    public void consumeIngestionReportActionPromote(IngestionReportActionMsg message) {
        log.info("Received ingestion report job with id: {} for promote offers", message.getIngestionReportId());
        try {
            offerInfraKafkaConsumerService.processIngestionReportPromoteAction(message.getIngestionReportId());
        } catch (Exception e) {
            log.error("Critical error processing report with id: {}. Triggering Kafka retry...", message.getIngestionReportId(), e);
            // We wrap and rethrow the exception.
            // By letting it propagate, Kafka's ErrorHandler will catch it.
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to process consumed job report with id: " + message.getIngestionReportId() + " for promote offers")
                    .cause(e)
                    .build();
        }
    }
}