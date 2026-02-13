package net.liquidcars.ingestion.infra.postgresql.entity.report;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionProcessType;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.JsonStringListConverter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ingestion_reports")
public class IngestionReportEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "process_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionProcessType processType;

    @Column(name = "batch_job_id", unique = true)
    private UUID batchJobId;

    @Column(name = "requester_participant_id")
    private UUID requesterParticipantId;

    @Column(name = "inventory_id")
    private UUID inventoryId;

    @Column(name = "external_request_id")
    private String externalRequestId;

    @Column(name = "publication_date")
    private OffsetDateTime publicationDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionBatchStatus status;

    @Column(name = "dump_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionDumpType dumpType;

    @Column(name = "read_count")
    private long readCount;

    @Column(name = "write_count")
    private long writeCount;

    @Column(name = "skip_count")
    private long skipCount;

    @Convert(converter = JsonStringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_external_ids", columnDefinition = "jsonb")
    private List<ExternalIdInfoJSONEntity> failedExternalIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ids_for_delete", columnDefinition = "jsonb")
    private List<String> idsForDelete;

    @Builder.Default
    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}