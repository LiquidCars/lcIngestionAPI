package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IngestionReportDto {
    private String jobId;
    private String status;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<UUID> failedExternalIds;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
}