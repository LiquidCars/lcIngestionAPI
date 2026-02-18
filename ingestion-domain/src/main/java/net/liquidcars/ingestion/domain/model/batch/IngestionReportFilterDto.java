package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.SortDirection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class IngestionReportFilterDto {
    private Integer page;
    private Integer size;
    private IngestionReportSortField sortBy;
    private SortDirection sortDirection;
    private IngestionProcessType processType;
    private UUID requesterParticipantId;
    private UUID inventoryId;
    private String externalRequestId;
    private IngestionBatchStatus status;
    private IngestionDumpType dumpType;
    private Boolean processed;
    private OffsetDateTime createdFrom;
    private OffsetDateTime createdTo;
    private OffsetDateTime updatedFrom;
    private OffsetDateTime updatedTo;
}