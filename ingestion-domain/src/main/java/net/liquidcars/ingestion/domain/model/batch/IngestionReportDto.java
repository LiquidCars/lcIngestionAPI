package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IngestionReportDto {
    private UUID id;
    private IngestionProcessType processType;
    private UUID batchJobId;
    private UUID requesterParticipantId;
    private UUID inventoryId;
    private String externalRequestId;
    private OffsetDateTime publicationDate;
    private IngestionBatchStatus status;
    private IngestionDumpType dumpType;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<String> failedExternalIds;
    private boolean processed = false;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}