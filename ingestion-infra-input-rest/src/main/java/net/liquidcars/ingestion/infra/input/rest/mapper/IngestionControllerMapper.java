package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.CarOfferSellerTypeEnumDto;
import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportPageDto;
import net.liquidcars.ingestion.infra.input.rest.model.*;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface IngestionControllerMapper {

    @Mapping(target = "offers", expression = "java(toOfferDtoList(ingestionPayload.getOffers(), participantId, inventoryId))")
    IngestionPayloadDto toIngestionPayloadDto(IngestionPayload ingestionPayload, UUID participantId, UUID inventoryId);

    @Named("toOfferDtoWithParticipant")
    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "lastUpdated", expression = "java(System.currentTimeMillis())")
    @Mapping(target = "participantId", source = "participantId")
    @Mapping(target = "inventoryId", source = "inventoryId")
    OfferDto toOfferDto(OfferRequest offerRequest, UUID participantId, UUID inventoryId);

    default List<OfferDto> toOfferDtoList(List<OfferRequest> offerRequestList, UUID participantId, UUID inventoryId) {
        if (offerRequestList == null) {
            return null;
        }
        return offerRequestList.stream()
                .map(offerRequest -> toOfferDto(offerRequest, participantId,  inventoryId))
                .collect(Collectors.toList());
    }

    @ValueMappings({
            @ValueMapping(source = "PROFESSIONALSELLER", target = "usedCar_ProfessionalSeller"),
            @ValueMapping(source = "PRIVATESELLER", target = "usedCar_PrivateSeller")
    })
    CarOfferSellerTypeEnumDto toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum carOfferSellerTypeEnum);

    IngestionReport toIngestionReport(IngestionReportDto ingestionReportDto);

    List<IngestionReport> toIngestionReportList(List<IngestionReportDto> ingestionReportDtoList);

    IngestionReportPage toIngestionReportPage(IngestionReportPageDto ingestionReportPageDto);
}
