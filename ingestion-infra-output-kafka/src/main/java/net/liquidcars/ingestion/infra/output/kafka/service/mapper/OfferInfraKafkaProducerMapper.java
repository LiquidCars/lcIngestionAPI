package net.liquidcars.ingestion.infra.output.kafka.service.mapper;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferInfraKafkaProducerMapper {
    OfferMsg toOfferMsg(OfferDto offerDto);
}
