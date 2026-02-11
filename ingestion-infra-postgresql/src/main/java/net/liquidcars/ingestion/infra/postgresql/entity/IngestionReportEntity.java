package net.liquidcars.ingestion.infra.postgresql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.JsonStringListConverter;
import org.hibernate.annotations.JdbcTypeCode;
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
    @Column(name = "job_id")
    private UUID jobId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionBatchStatus status;

    @Column(name = "read_count")
    private long readCount;

    @Column(name = "write_count")
    private long writeCount;

    @Column(name = "skip_count")
    private long skipCount;

    @Convert(converter = JsonStringListConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "failed_external_ids", columnDefinition = "jsonb")
    private List<String> failedExternalIds;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Builder.Default
    @Column(name = "processed", nullable = false)
    private boolean processed = false;
}
