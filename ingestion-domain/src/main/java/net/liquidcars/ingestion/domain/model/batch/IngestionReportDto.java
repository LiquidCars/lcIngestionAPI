package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;

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
    private Integer readCount;
    private Integer writeCount;
    private Integer skipCount;
    private List<ExternalIdInfoDto> failedExternalIds;
    private List<String> idsForDelete;
    private boolean processed = false;
    private boolean promoted = false;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}