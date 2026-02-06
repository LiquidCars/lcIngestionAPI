package net.liquidcars.ingestion.application.service.parser.mapper;

import net.liquidcars.ingestion.application.service.parser.model.JSON.OfferJSONModel;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferParserMapper {

    OfferDto toOfferDto(OfferJSONModel offerJSONModel);

    OfferDto toOfferDto(OfferXMLModel offerXMLModel);

}
