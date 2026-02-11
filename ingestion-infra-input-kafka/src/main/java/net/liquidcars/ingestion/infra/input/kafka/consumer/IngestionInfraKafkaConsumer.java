package net.liquidcars.ingestion.infra.input.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.infra.input.kafka.service.mapper.OfferInfraKafkaConsumerMapper;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportMsg;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionInfraKafkaConsumer {

    private final OfferInfraKafkaConsumerMapper offerInfraKafkaConsumerMapper;
    private final IOfferInfraKafkaConsumerService offerInfraKafkaConsumerService;

    @KafkaListener(
            topics = "liquidcars.ingestion.event.report.executed-action.0",
            groupId = "liquidcars-ingestion-group"
    )
    public void consumeOffer(IngestionReportMsg message) {
        log.info("Received ingestion report job with id: {}", message.getJobId());
        try {
            IngestionBatchReportDto reportDto = offerInfraKafkaConsumerMapper.toIngestionReportDto(message);
            offerInfraKafkaConsumerService.processIngestionReport(reportDto);
        } catch (Exception e) {
            log.error("Critical error processing report with id: {}. Triggering Kafka retry...", message.getJobId(), e);
            // We wrap and rethrow the exception.
            // By letting it propagate, Kafka's ErrorHandler will catch it.
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("Failed to process consumed job report with id: " + message.getJobId())
                    .cause(e)
                    .build();
        }
    }
}