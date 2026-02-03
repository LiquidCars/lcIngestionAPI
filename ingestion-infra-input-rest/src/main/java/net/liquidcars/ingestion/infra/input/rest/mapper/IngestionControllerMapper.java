package net.liquidcars.ingestion.infra.input.rest.mapper;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.input.rest.model.OfferStatus;
import net.liquidcars.ingestion.infra.input.rest.model.VehicleType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;

import java.util.List;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionControllerMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID().toString())")
    OfferDto toOfferDto(OfferRequest offerRequest);

    List<OfferDto> toOfferDtoList(List<OfferRequest> offerRequestList);

    OfferDto.VehicleTypeDto mapVehicleType(VehicleType source);

    OfferDto.OfferStatusDto mapOfferStatus(OfferStatus source);

}
