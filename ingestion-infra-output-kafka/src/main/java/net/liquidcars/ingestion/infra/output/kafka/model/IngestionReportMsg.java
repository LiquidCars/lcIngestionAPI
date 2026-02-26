package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionProcessType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionReportMsg {
    private UUID id;
    private IngestionProcessType processType;
    private UUID batchJobId;
    private UUID requesterParticipantId;
    private UUID inventoryId;
    private String externalRequestId;
    private OffsetDateTime publicationDate;
    private IngestionBatchStatus status;
    private IngestionDumpType dumpType;
    private Integer readCount;
    private Integer writeCount;
    private Integer skipCount;
    private List<ExternalIdInfoMsg> failedExternalIds;
    private List<String> idsForDelete;
    private List<UUID> activeBookedOfferIds;
    private boolean processed;
    private boolean promoted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID workflowId;
}