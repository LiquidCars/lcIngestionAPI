package net.liquidcars.ingestion.infra.output.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionReportKafkaPublisher {

    private final KafkaTemplate<String, IngestionReportMsg> kafkaTemplate;
    private static final String UPDATED_REPORT_TOPIC = "liquidcars.ingestion.event.report.updated-action.0";

    public void sendIngestionReport(IngestionReportMsg ingestionReport) {
        try {
            log.info("Sending kafka topic {}: {}", UPDATED_REPORT_TOPIC, ingestionReport);
            kafkaTemplate.send(UPDATED_REPORT_TOPIC, ingestionReport.getId().toString(), ingestionReport).get();
        } catch (Exception e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Error sending ingestion report with id: "+ ingestionReport.getId())
                    .cause(e)
                    .build();
        }
    }
}
