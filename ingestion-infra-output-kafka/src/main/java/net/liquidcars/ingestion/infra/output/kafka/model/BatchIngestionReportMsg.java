package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchIngestionReportMsg {
    private String jobId;
    private String status;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<ExternalIdInfoMsg> failedExternalIds;
    private String startTime;
    private String endTime;
}