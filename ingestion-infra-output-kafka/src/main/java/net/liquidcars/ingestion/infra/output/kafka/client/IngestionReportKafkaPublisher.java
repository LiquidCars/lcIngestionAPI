package net.liquidcars.ingestion.infra.output.kafka.client;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IngestionReportKafkaPublisher {

    private final KafkaTemplate<String, IngestionReportMsg> kafkaTemplate;
    private static final String CREATE_REPORT_TOPIC = "liquidcars.ingestion.event.report.executed-action.0";

    public void sendIngestionReport(IngestionReportMsg ingestionReport) {
        kafkaTemplate.send(CREATE_REPORT_TOPIC, ingestionReport.getJobId(), ingestionReport);
    }
}
