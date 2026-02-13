package net.liquidcars.ingestion.application.service.batch.mapper;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.batch.core.BatchStatus;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionBatchMapper {
    //Spring Batch → Domain
    IngestionBatchStatus toIngestionBatchStatus(BatchStatus status);
}
