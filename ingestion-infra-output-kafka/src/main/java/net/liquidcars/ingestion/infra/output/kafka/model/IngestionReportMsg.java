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
    private String publicationDate;
    private String status;
    private IngestionDumpType dumpType;
    private Integer readCount;
    private Integer writeCount;
    private Integer skipCount;
    private List<ExternalIdInfoMsg> failedExternalIds;
    private List<String> idsForDelete;
    private boolean processed;
    private boolean promoted;
    private String createdAt;
    private String updatedAt;
}