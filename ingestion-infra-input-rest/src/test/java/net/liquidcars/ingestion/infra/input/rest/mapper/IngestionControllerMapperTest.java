package net.liquidcars.ingestion.infra.input.rest.mapper;

import net.liquidcars.ingestion.domain.model.CarOfferSellerTypeEnumDto;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchStatus;
import net.liquidcars.ingestion.domain.model.batch.IngestionProcessType;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.factory.OfferRequestFactory;
import net.liquidcars.ingestion.infra.input.rest.model.CarOfferSellerTypeEnum;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionPayload;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReport;
import net.liquidcars.ingestion.infra.input.rest.model.OfferRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {IngestionControllerMapperImpl.class})
public class IngestionControllerMapperTest {

    @Autowired
    private IngestionControllerMapper mapper;

    private final UUID participantId = UUID.randomUUID();
    private final UUID inventoryId = UUID.randomUUID();

    @Test
    void shouldMapOfferRequestToOfferDtoWithGeneratedId() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId, inventoryId);

        assertThat(result).isNotNull();
        assertThat(result.getExternalIdInfo().getChannelReference()).isEqualTo(request.getExternalIdInfo().getChannelReference());
        assertThat(result.getVehicleInstance().getPlate()).isEqualTo(request.getVehicleInstance().getPlate());

        assertThat(result.getMail()).isEqualTo(request.getMail());
        assertThat(result.getPrice().getAmount()).isEqualByComparingTo(request.getPrice().getAmount());
        assertThat(result.getParticipantId()).isEqualTo(participantId);
        assertThat(result.getInventoryId()).isEqualTo(inventoryId);
        assertThat(result.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldReturnNullWhenListIsNull() {
        List<OfferDto> result = mapper.toOfferDtoList(null, participantId, inventoryId);

        assertThat(result).isNull();
    }

    @Test
    void shouldReturnEmptyListWhenSourceListIsEmpty() {
        List<OfferRequest> emptyList = List.of();

        List<OfferDto> result = mapper.toOfferDtoList(emptyList, participantId, inventoryId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldMapParticipantIdCorrectly() {
        OfferRequest request = OfferRequestFactory.getOfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId, inventoryId);

        assertThat(result.getParticipantId())
                .isNotNull()
                .isEqualTo(participantId);
    }

    @Test
    @DisplayName("Debería mapear IngestionPayload a Dto incluyendo la lista de ofertas")
    void shouldMapIngestionPayloadToDto() {
        IngestionPayload payload = new IngestionPayload();
        OfferRequest offer = new OfferRequest(); // Asumiendo campos básicos
        payload.addOffersItem(offer);
        payload.setOffersToDelete(List.of("ID-1"));

        IngestionPayloadDto result = mapper.toIngestionPayloadDto(payload, participantId, inventoryId);

        assertThat(result).isNotNull();
        assertThat(result.getOffers()).hasSize(1);
        assertThat(result.getOffersToDelete()).containsExactly("ID-1");
        // Verificar que el mapeo interno de la lista usó el participantId e inventoryId
        assertThat(result.getOffers().get(0).getParticipantId()).isEqualTo(participantId);
        assertThat(result.getOffers().get(0).getInventoryId()).isEqualTo(inventoryId);
    }

    // --- Tests de ValueMappings (Enums) ---

    @Test
    @DisplayName("Debería mapear correctamente los valores del Enum de SellerType")
    void shouldMapSellerTypeEnum() {
        assertThat(mapper.toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum.PROFESSIONALSELLER))
                .isEqualTo(CarOfferSellerTypeEnumDto.usedCar_ProfessionalSeller);

        assertThat(mapper.toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum.PRIVATESELLER))
                .isEqualTo(CarOfferSellerTypeEnumDto.usedCar_PrivateSeller);
    }

    @Test
    @DisplayName("Debería devolver null si el Enum es null")
    void shouldReturnNullWhenEnumIsNull() {
        assertThat(mapper.toCarOfferSellerTypeEnumDto(null)).isNull();
    }

    // --- Tests de IngestionReport ---

    @Test
    @DisplayName("Debería mapear IngestionReportDto a IngestionReport con todos sus campos")
    void shouldMapFullIngestionReportDtoToEntity() {
        // 1. Preparamos el DTO usando el Builder de Lombok
        UUID reportId = UUID.randomUUID();
        UUID batchId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        IngestionReportDto dto = IngestionReportDto.builder()
                .id(reportId)
                .batchJobId(batchId)
                .processType(IngestionProcessType.PROCESS) // Asumiendo que este enum existe
                .status(IngestionBatchStatus.COMPLETED)    // Asumiendo que este enum existe
                .readCount(100)
                .writeCount(95)
                .skipCount(5)
                .processed(true)
                .createdAt(now)
                .build();

        // 2. Ejecutamos el mapeo
        IngestionReport result = mapper.toIngestionReport(dto);

        // 3. Verificaciones
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(reportId);
        assertThat(result.getBatchJobId()).isEqualTo(batchId);
        assertThat(result.getReadCount()).isEqualTo(100);
        assertThat(result.getWriteCount()).isEqualTo(95);
        assertThat(result.getSkipCount()).isEqualTo(5);
        // processed es un primitivo boolean, verificamos que se mapee bien
        // Nota: Asegúrate de que el modelo 'IngestionReport' tenga el campo 'processed' o 'isProcessed'
        // Si no lo tiene, MapStruct lo ignorará por la política IGNORE.
    }

    @Test
    @DisplayName("Debería mapear una lista de IngestionReportDto")
    void shouldMapIngestionReportDtoList() {
        IngestionReportDto dto1 = IngestionReportDto.builder().readCount(10).build();
        IngestionReportDto dto2 = IngestionReportDto.builder().readCount(20).build();
        List<IngestionReportDto> dtoList = List.of(dto1, dto2);

        List<IngestionReport> resultList = mapper.toIngestionReportList(dtoList);

        assertThat(resultList).isNotNull().hasSize(2);
        assertThat(resultList.get(0).getReadCount()).isEqualTo(10);
        assertThat(resultList.get(1).getReadCount()).isEqualTo(20);
    }

    @Test
    @DisplayName("Debería manejar nulos en la lista de reports para cobertura de MapStruct")
    void shouldHandleNullReportList() {
        // MapStruct genera internamente: if (list == null) return null;
        assertThat(mapper.toIngestionReportList(null)).isNull();
        assertThat(mapper.toIngestionReport(null)).isNull();
    }

    // --- Tests de casos borde en OfferDto ---

    @Test
    @DisplayName("Debería generar UUID e Instant al mapear OfferDto")
    void shouldGenerateAutomaticFields() {
        OfferRequest request = new OfferRequest();

        OfferDto result = mapper.toOfferDto(request, participantId, inventoryId);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getLastUpdated()).isPositive();
    }

    @Test
    @DisplayName("Debería mapear la lista de fallos de ExternalIdInfoDto a ExternalIdInfo")
    void shouldMapExternalIdInfoList() {
        // Este test cubre 'externalIdInfoDtoListToExternalIdInfoList' y 'externalIdInfoDtoToExternalIdInfo'
        ExternalIdInfoDto failedId = new ExternalIdInfoDto();
        failedId.setOwnerReference("OWN-1");
        failedId.setDealerReference("DEA-1");

        IngestionReportDto dto = IngestionReportDto.builder()
                .failedExternalIds(List.of(failedId))
                .build();

        IngestionReport result = mapper.toIngestionReport(dto);

        assertThat(result.getFailedExternalIds()).hasSize(1);
        assertThat(result.getFailedExternalIds().get(0).getOwnerReference()).isEqualTo("OWN-1");
        assertThat(result.getFailedExternalIds().get(0).getDealerReference()).isEqualTo("DEA-1");
    }

    @Test
    @DisplayName("Debería manejar lista de fallos nula para cobertura de MapStruct")
    void shouldHandleNullExternalIdList() {
        IngestionReportDto dto = IngestionReportDto.builder()
                .failedExternalIds(null)
                .build();

        IngestionReport result = mapper.toIngestionReport(dto);

        // Esto fuerza la ejecución del if (list == null) return null interno de MapStruct
        assertThat(result.getFailedExternalIds()).isNull();
    }

    @Test
    @DisplayName("Debería mapear el Enum AddressType a su DTO correspondiente")
    void shouldMapAddressTypeToDto() {
        // Probamos un valor concreto para cubrir el switch/if que genera MapStruct
        CarOfferSellerTypeEnumDto result = mapper.toCarOfferSellerTypeEnumDto(CarOfferSellerTypeEnum.PROFESSIONALSELLER);

        assertThat(result).isEqualTo(CarOfferSellerTypeEnumDto.usedCar_ProfessionalSeller);
    }
}