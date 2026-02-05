package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
public class IngestionReportDto {
    private String jobId;
    private String status;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<String> failedExternalIds;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}