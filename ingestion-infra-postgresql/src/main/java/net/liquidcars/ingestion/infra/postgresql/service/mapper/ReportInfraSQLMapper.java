package net.liquidcars.ingestion.infra.postgresql.service.mapper;

import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { OffsetDateTime.class })
public interface ReportInfraSQLMapper {

    IngestionReportEntity toIngestionReportEntity(IngestionReportDto ingestionReportDto);
    IngestionReportDto toIngestionReportDto(IngestionReportEntity ingestionReportEntity);
    List<IngestionReportDto> toIngestionReportDtoList(List<IngestionReportEntity> ingestionReportEntityList);
}
