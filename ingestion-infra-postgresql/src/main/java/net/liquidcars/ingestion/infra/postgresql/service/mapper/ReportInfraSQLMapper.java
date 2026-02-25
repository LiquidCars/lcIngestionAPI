package net.liquidcars.ingestion.infra.postgresql.service.mapper;

import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportPageDto;
import net.liquidcars.ingestion.infra.postgresql.entity.report.IngestionReportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { OffsetDateTime.class })
public interface ReportInfraSQLMapper {

    IngestionReportEntity toIngestionReportEntity(IngestionReportDto ingestionReportDto);

    IngestionReportDto toIngestionReportDto(IngestionReportEntity ingestionReportEntity);

    List<IngestionReportDto> toIngestionReportDtoList(List<IngestionReportEntity> ingestionReportEntityList);

    default IngestionReportPageDto toIngestionReportPageDto(Page<IngestionReportEntity> entityPage) {
        if (entityPage == null) {
            return null;
        }

        return IngestionReportPageDto.builder()
                .content(entityPage.getContent().stream()
                        .map(this::toIngestionReportDto)
                        .toList())
                .totalElements(entityPage.getTotalElements())
                .totalPages(entityPage.getTotalPages())
                .size(entityPage.getSize())
                .number(entityPage.getNumber())
                .last(entityPage.isLast())
                .build();
    }
}
