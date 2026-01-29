package net.liquidcars.ingestion.application.service.parser.mapper;

import net.liquidcars.ingestion.application.service.parser.model.OfferJSONModel;
import net.liquidcars.ingestion.application.service.parser.model.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferParserMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    OfferDto toOfferDto(OfferJSONModel offerJSONModel);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    OfferDto toOfferDto(OfferXMLModel offerXMLModel);

}
