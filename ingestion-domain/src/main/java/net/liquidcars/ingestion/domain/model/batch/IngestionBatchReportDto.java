package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IngestionBatchReportDto {
    private UUID jobId;
    private IngestionBatchStatus status;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<UUID> failedExternalIds;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private boolean processed;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}