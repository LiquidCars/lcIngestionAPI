package net.liquidcars.ingestion.infra.output.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.output.kafka.model.BatchIngestionReportMsg;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchIngestionReportKafkaPublisher {

    private final KafkaTemplate<String, BatchIngestionReportMsg> kafkaTemplate;
    private static final String CREATE_REPORT_TOPIC = "liquidcars.ingestion.event.batchreport.executed-action.0";

    public void sendBatchIngestionReport(BatchIngestionReportMsg ingestionReport) {
        try {
            log.info("Sending kafka topic {}: {}", CREATE_REPORT_TOPIC, ingestionReport);
            kafkaTemplate.send(CREATE_REPORT_TOPIC, ingestionReport.getJobId(), ingestionReport).get();
        } catch (Exception e) {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Error sending batch report for job with id: "+ ingestionReport.getJobId())
                    .cause(e)
                    .build();
        }
    }
}
