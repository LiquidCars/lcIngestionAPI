package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class IngestionReportMsg {
    private String jobId;
    private String status;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<String> failedExternalIds;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}