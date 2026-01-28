package net.liquidcars.ingestion.infra.input.rest.mapper;
import net.liquidcars.ingestion.domain.model.OfferDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;

import java.util.List;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionControllerMapper {

    OfferDto toOfferDto(OfferRequest offerRequest);

    List<OfferDto> toOfferDtoList(List<OfferRequest> offerRequestList);

}
