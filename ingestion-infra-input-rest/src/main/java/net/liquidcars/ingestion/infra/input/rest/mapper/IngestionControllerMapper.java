package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.CarOfferSellerTypeEnumDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.input.rest.model.CarOfferSellerTypeEnum;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.mapstruct.*;

import java.util.List;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionControllerMapper {

    OfferDto toOfferDto(OfferRequest offerRequest);

    List<OfferDto> toOfferDtoList(List<OfferRequest> offerRequestList);

    @ValueMappings({
            @ValueMapping(source = "PROFESSIONALSELLER", target = "usedCar_ProfessionalSeller"),
            @ValueMapping(source = "PRIVATESELLER", target = "usedCar_PrivateSeller")
    })
    CarOfferSellerTypeEnumDto toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum carOfferSellerTypeEnum);

}
