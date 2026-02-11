package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.CarOfferSellerTypeEnumDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.model.CarOfferSellerTypeEnum;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.mapstruct.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionControllerMapper {

    OfferDto toOfferDto(OfferRequest offerRequest, @Context IContextService context);

    List<OfferDto> toOfferDtoList(List<OfferRequest> offerRequestList, @Context IContextService context);

    @ValueMappings({
            @ValueMapping(source = "PROFESSIONALSELLER", target = "usedCar_ProfessionalSeller"),
            @ValueMapping(source = "PRIVATESELLER", target = "usedCar_PrivateSeller")
    })
    CarOfferSellerTypeEnumDto toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum carOfferSellerTypeEnum);

    @AfterMapping
    default void enrichOfferDto(@MappingTarget OfferDto offerDto, @Context IContextService contextService) {
        LCContext context = contextService.getContext();
        if (context != null && context.getParticipantId() != null) {
            offerDto.setParticipantId(UUID.fromString(context.getParticipantId()));
        }
        offerDto.setLastUpdated(Instant.now().getEpochSecond());
    }

}
