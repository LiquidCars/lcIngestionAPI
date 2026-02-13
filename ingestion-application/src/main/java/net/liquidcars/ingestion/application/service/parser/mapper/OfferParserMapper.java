package net.liquidcars.ingestion.application.service.parser.mapper;

import net.liquidcars.ingestion.application.service.parser.model.JSON.OfferJSONModel;
import net.liquidcars.ingestion.application.service.parser.model.XML.ExternalIdInfoXMLModel;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OfferParserMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    OfferDto toOfferDto(OfferJSONModel offerJSONModel);

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    OfferDto toOfferDto(OfferXMLModel offerXMLModel);

    ExternalIdInfoDto toExternalIdInfoDto(ExternalIdInfoXMLModel externalIdInfoXMLModel);

}
