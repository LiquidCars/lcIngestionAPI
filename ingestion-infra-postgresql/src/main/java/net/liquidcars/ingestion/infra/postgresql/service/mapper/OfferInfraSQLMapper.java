package net.liquidcars.ingestion.infra.postgresql.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.infra.postgresql.entity.IngestionReportEntity;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferInfraSQLMapper {

    OfferEntity toOfferEntity(OfferDto offer);

    IngestionReportEntity toIngestionReportEntity(IngestionReportDto ingestionReportDto);
}
