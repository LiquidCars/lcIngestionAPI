package net.liquidcars.ingestion.infra.postgresql.service.mapper;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { OffsetDateTime.class })
public interface BatchReportInfraSQLMapper {

    IngestionReportEntity toIngestionReportEntity(IngestionBatchReportDto ingestionBatchReportDto);
    List<IngestionBatchReportDto> toIngestionReportDtoList(List<IngestionReportEntity> ingestionReportEntityList);
}
