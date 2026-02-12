package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionReportMsg {
    private String id;
    private String processType;
    private String batchJobId;
    private UUID requesterParticipantId;
    private UUID inventoryId;
    private String externalRequestId;
    private OffsetDateTime publicationDate;
    private String status;
    private IngestionDumpType dumpType;
    private long readCount;
    private long writeCount;
    private long skipCount;
    private List<ExternalIdInfoMsg> failedExternalIds;
    private boolean processed = false;
    private String createdAt;
    private String updatedAt;
}