package net.liquidcars.ingestion.infra.output.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportMsg;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferInfraKafkaProducerMapper {
    DateTimeFormatter OFFSET_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    OfferMsg toOfferMsg(OfferDto offerDto);

    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    IngestionReportMsg toIngestionReportMsg(IngestionReportDto ingestionReportDto);

    default String map(OffsetDateTime value) {
        return value == null ? null : value.format(OFFSET_FORMATTER);
    }
}
