package net.liquidcars.ingestion.infra.output.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportResponseActionMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionReportDeleteActionKafkaPublisher {

    private final KafkaTemplate<String, IngestionReportResponseActionMsg> kafkaTemplate;
    private static final String REPORT_DELETE_ACTION_RESULT_TOPIC = "liquidcars.ingestion.event.report.delete-action-result.0";

    public void sendIngestionReportResponseAction(IngestionReportResponseActionMsg ingestionReportResponseActionMsg) {
        try {
            log.info("Sending kafka topic {}: {}", REPORT_DELETE_ACTION_RESULT_TOPIC, ingestionReportResponseActionMsg);
            kafkaTemplate.send(REPORT_DELETE_ACTION_RESULT_TOPIC, ingestionReportResponseActionMsg.getIngestionReportId().toString(), ingestionReportResponseActionMsg).get();
        } catch (Exception e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Error sending delete action response for ingestion report with id: " + ingestionReportResponseActionMsg.getIngestionReportId())
                    .cause(e)
                    .build();
        }
    }
}
