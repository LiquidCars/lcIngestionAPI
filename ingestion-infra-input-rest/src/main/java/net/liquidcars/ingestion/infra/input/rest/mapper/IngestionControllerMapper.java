package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.CarOfferSellerTypeEnumDto;
import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.input.rest.model.CarOfferSellerTypeEnum;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionPayload;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionControllerMapper {

    @Mapping(target = "offers", expression = "java(toOfferDtoList(ingestionPayload.getOffers(), participantId))")
    IngestionPayloadDto toIngestionPayloadDto(IngestionPayload ingestionPayload, UUID participantId);

    @Named("toOfferDtoWithParticipant")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "lastUpdated", expression = "java(java.time.Instant.now().getEpochSecond())")
    @Mapping(target = "participantId", source = "participantId")
    OfferDto toOfferDto(OfferRequest offerRequest, UUID participantId);

    default List<OfferDto> toOfferDtoList(List<OfferRequest> offerRequestList, UUID participantId) {
        if (offerRequestList == null) {
            return null;
        }
        return offerRequestList.stream()
                .map(offerRequest -> toOfferDto(offerRequest, participantId))
                .collect(Collectors.toList());
    }

    @ValueMappings({
            @ValueMapping(source = "PROFESSIONALSELLER", target = "usedCar_ProfessionalSeller"),
            @ValueMapping(source = "PRIVATESELLER", target = "usedCar_PrivateSeller")
    })
    CarOfferSellerTypeEnumDto toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum carOfferSellerTypeEnum);

}
