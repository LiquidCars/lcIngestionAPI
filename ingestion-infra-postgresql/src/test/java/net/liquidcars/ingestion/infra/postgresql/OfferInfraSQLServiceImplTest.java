package net.liquidcars.ingestion.infra.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.liquidcars.ingestion.domain.model.CarOfferResourceDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.VehicleInstanceDto;
import net.liquidcars.ingestion.domain.model.VehicleModelDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.postgresql.entity.*;
import net.liquidcars.ingestion.infra.postgresql.repository.*;
import net.liquidcars.ingestion.infra.postgresql.service.OfferInfraSQLServiceImpl;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferInfraSQLServiceImplTest {

    @Mock private OfferSQLRepository offerSqlRepository;
    @Mock private VehicleModelSQLRepository vehicleModelRepository;
    @Mock private CarOfferResourceRepository carOfferResourceRepository;
    @Mock private ParticipantAddressEntityRepository participantAddressEntityRepository;
    @Mock private CarInstanceEquipmentEntityRepository carInstanceEquipmentEntityRepository;
    @Mock private OfferInfraSQLMapper mapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OfferInfraSQLServiceImpl offerService;

    private OfferDto sampleOfferDto;
    private UUID inventoryId;

    @BeforeEach
    void setUp() {
        inventoryId = UUID.randomUUID();
        sampleOfferDto = OfferDtoFactory.getOfferDto();
        lenient().when(mapper.toParticipantAddressEntity(any()))
                .thenReturn(new ParticipantAddressEntity());
    }

    @Test
    @DisplayName("Should create a new offer when it doesn't exist")
    void processOffer_NewOffer_ShouldSave() {
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());

        OfferEntity entityToSave = new OfferEntity();
        entityToSave.setVehicleInstance(new VehicleInstanceEntity());

        when(mapper.toEntity(any())).thenReturn(entityToSave);
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(entityToSave);

        offerService.processOffer(sampleOfferDto);

        verify(offerSqlRepository).saveAndFlush(any(OfferEntity.class));
    }

    @Test
    @DisplayName("Should update offer only if incoming date is newer")
    void processOffer_ExistingOffer_NewerDate_ShouldUpdate() {
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime newDate = OffsetDateTime.now();

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setCreatedAt(oldDate);
        existingEntity.setVehicleInstance(new VehicleInstanceEntity());

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existingEntity));

        when(mapper.mapEpoch(anyLong())).thenReturn(newDate);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());
        lenient().when(participantAddressEntityRepository.findById(any())).thenReturn(Optional.empty());

        offerService.processOffer(sampleOfferDto);

        verify(offerSqlRepository).save(existingEntity);
    }

    @Test
    @DisplayName("Should NOT update offer if incoming date is older")
    void processOffer_ExistingOffer_OlderDate_ShouldDoNothing() {
        OffsetDateTime existingDate = OffsetDateTime.now().plusDays(1);
        OffsetDateTime incomingDate = OffsetDateTime.now();

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setCreatedAt(existingDate);

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existingEntity));

        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        offerService.processOffer(sampleOfferDto);

        verify(offerSqlRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteByReferences should return 0 if references list is empty")
    void deleteByReferences_EmptyList_ReturnsZero() {
        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, Collections.emptyList());

        assertThat(result).isZero();
        verifyNoInteractions(offerSqlRepository);
    }

    @Test
    @DisplayName("updateFullOffer should create new address if it doesn't exist in DB")
    void updateFullOffer_AddressNotFound_ShouldCreateNew() {
        OffsetDateTime incomingDate = OffsetDateTime.now();
        OffsetDateTime existingDate = incomingDate.minusDays(1);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setCreatedAt(existingDate);
        existingEntity.setVehicleInstance(new VehicleInstanceEntity());

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existingEntity));

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        when(participantAddressEntityRepository.findById(any())).thenReturn(Optional.empty());

        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);

        ParticipantAddressEntity newAddress = new ParticipantAddressEntity();
        when(mapper.toParticipantAddressEntity(any())).thenReturn(newAddress);

        offerService.processOffer(sampleOfferDto);

        verify(participantAddressEntityRepository).save(newAddress);
        verify(offerSqlRepository).save(existingEntity);
    }

    @Test
    @DisplayName("processOffer should create new VehicleModel if not found in DB")
    void processOffer_ModelNotFound_ShouldCreateNewModel() {
        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.empty());

        VehicleModelEntity newModel = new VehicleModelEntity();
        when(mapper.toVehicleModelEntity(any())).thenReturn(newModel);
        when(vehicleModelRepository.save(any(VehicleModelEntity.class))).thenReturn(newModel);

        OfferEntity offerEntity = new OfferEntity();
        offerEntity.setVehicleInstance(new VehicleInstanceEntity());
        when(mapper.toEntity(any())).thenReturn(offerEntity);
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(offerEntity);

        offerService.processOffer(sampleOfferDto);

        verify(vehicleModelRepository).save(any(VehicleModelEntity.class));
        assertThat(newModel.isEnabled()).isTrue();
        verify(offerSqlRepository).saveAndFlush(any(OfferEntity.class));
    }

    @Test
    @DisplayName("processOffer: Debe asignar tipo 'Other' a los equipamientos si el tipo es nulo")
    void processOffer_Equipments_ShouldSetDefaultTypeIfNull() {
        OfferEntity offerEntity = new OfferEntity();
        VehicleInstanceEntity vehicleInstance = new VehicleInstanceEntity();
        vehicleInstance.setId(123L);
        offerEntity.setVehicleInstance(vehicleInstance);

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(mapper.toEntity(any())).thenReturn(offerEntity);
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(offerEntity);
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        net.liquidcars.ingestion.infra.postgresql.entity.CarInstanceEquipmentEntity equipment =
                new net.liquidcars.ingestion.infra.postgresql.entity.CarInstanceEquipmentEntity();
        equipment.setType(null);

        when(mapper.toCarInstanceEquipmentEntityList(any())).thenReturn(new java.util.ArrayList<>(List.of(equipment)));

        offerService.processOffer(sampleOfferDto);

        assertThat(equipment.getType()).isNotNull();
        assertThat(equipment.getType().getId()).isEqualTo("Other");

        assertThat(equipment.getVehicleInstance()).isEqualTo(vehicleInstance);

        verify(carInstanceEquipmentEntityRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("updateFullOffer should update existing address (covers lambda$updateFullOffer$4)")
    void updateFullOffer_AddressExists_ShouldUpdate() {
        OffsetDateTime incomingDate = OffsetDateTime.now();

        OfferEntity existingOffer = new OfferEntity();
        existingOffer.setCreatedAt(incomingDate.minusDays(1));
        existingOffer.setVehicleInstance(new VehicleInstanceEntity());

        ParticipantAddressEntity existingAddr = new ParticipantAddressEntity();

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existingOffer));

        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        when(participantAddressEntityRepository.findById(any())).thenReturn(Optional.of(existingAddr));

        offerService.processOffer(sampleOfferDto);

        verify(mapper).updateAddressFromDto(eq(sampleOfferDto.getPickUpAddress()), eq(existingAddr));

        verify(participantAddressEntityRepository, never()).save(any(ParticipantAddressEntity.class));
    }

    @Test
    @DisplayName("processOffer should throw LCIngestionException when repository fails")
    void processOffer_ShouldThrowException_WhenRepositoryFails() {
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection error"));

        assertThatThrownBy(() -> offerService.processOffer(sampleOfferDto))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("SQL persistence error");
    }

    @Test
    @DisplayName("convertUrlToBytes should return null if url is null")
    void convertUrlToBytes_ShouldReturnNull_WhenUrlIsNull() {
        CarOfferResourceDto resourceWithNullUrl = new CarOfferResourceDto();
        resourceWithNullUrl.setResource(null);
        sampleOfferDto.setResources(List.of(resourceWithNullUrl));

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(new OfferEntity());

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        offerService.processOffer(sampleOfferDto);

        verify(carOfferResourceRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("updateFullOffer should skip vehicle logic if vehicleInstance is null")
    void updateFullOffer_ShouldHandleNullVehicleInstance() {
        OffsetDateTime now = OffsetDateTime.now();
        OfferEntity existing = new OfferEntity();
        existing.setVehicleInstance(null);
        existing.setCreatedAt(now.minusDays(1));

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        when(mapper.mapEpoch(anyLong())).thenReturn(now);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        offerService.processOffer(sampleOfferDto);

        verify(offerSqlRepository).save(existing);

        verify(carInstanceEquipmentEntityRepository, never()).deleteByVehicleInstanceId(anyLong());
    }

    @Test
    @DisplayName("updateFullOffer should restore vehicle ID and update JSON offer text")
    void updateFullOffer_ShouldRestoreIdAndUpdateJson() {
        Long expectedVehicleId = 999L;
        OffsetDateTime now = OffsetDateTime.now();

        OfferEntity existing = new OfferEntity();
        existing.setCreatedAt(now.minusDays(1));

        VehicleInstanceEntity existingVehicle = new VehicleInstanceEntity();
        existingVehicle.setId(expectedVehicleId);
        existing.setVehicleInstance(existingVehicle);

        JsonOfferEntity existingJson = new JsonOfferEntity();
        existing.setJsonCarOffer(existingJson);

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(mapper.mapEpoch(anyLong())).thenReturn(now);

        Map<String, Object> mockJsonMap = new java.util.HashMap<>();
        mockJsonMap.put("key", "value");

        doReturn(mockJsonMap).when(objectMapper).convertValue(any(), eq(Map.class));

        lenient().when(participantAddressEntityRepository.findById(any()))
                .thenReturn(Optional.of(new ParticipantAddressEntity()));

        offerService.processOffer(sampleOfferDto);

        assertThat(existing.getVehicleInstance().getId()).isEqualTo(expectedVehicleId);
        assertThat(existing.getJsonCarOffer().getTexto()).isEqualTo(mockJsonMap);
        assertThat(existing.getJsonCarOffer().getCreatedAt()).isNotNull();

        verify(offerSqlRepository).save(existing);
    }

    @Test
    @DisplayName("updateFullOffer should generate random ID if existing vehicle instance ID is null")
    void updateFullOffer_ShouldGenerateRandomId_WhenExistingIdIsNull() {
        OffsetDateTime now = OffsetDateTime.now();
        OfferEntity existing = new OfferEntity();
        existing.setCreatedAt(now.minusDays(1));
        existing.setVehicleInstance(new VehicleInstanceEntity());

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        when(mapper.mapEpoch(anyLong())).thenReturn(now);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        offerService.processOffer(sampleOfferDto);

        assertThat(existing.getVehicleInstance().getId()).isNotNull();
        assertThat(existing.getVehicleInstance().getId()).isBetween(100_000_000L, 999_999_999L);
    }

    @Test
    @DisplayName("deleteOffersByInventoryIdExcludingIds: Cobertura de borrado delta")
    void deleteOffersByInventoryIdExcludingIds_Success_Coverage() {
        UUID inventoryId = UUID.randomUUID();
        List<UUID> idsToKeep = List.of(UUID.randomUUID());

        when(offerSqlRepository.deleteMainOfferDataExcluding(eq(inventoryId), anyList()))
                .thenReturn(10);

        long result = offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep);

        assertThat(result).isEqualTo(10L);
        verify(offerSqlRepository).deleteCarloanPreviewByInventoryExcluding(inventoryId, idsToKeep);
        verify(offerSqlRepository).deleteMainOfferDataExcluding(inventoryId, idsToKeep);
    }


    @Test
    @DisplayName("handleDeletionError: Debe capturar error de DB y lanzar LCIngestionException")
    void handleDeletionError_Coverage() {
        UUID inventoryId = UUID.randomUUID();
        doThrow(new RuntimeException("Database connection lost"))
                .when(offerSqlRepository).deleteCarloanPreviewByInventoryId(any());

        assertThatThrownBy(() -> offerService.deleteOffersByInventoryId(inventoryId))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Database error during offer deletion");
    }

    @Test
    @DisplayName("deleteOffersByInventoryId: Debe cubrir todas las llamadas secuenciales de borrado")
    void deleteOffersByInventoryId_FullCoverage() {
        UUID inventoryId = UUID.randomUUID();

        when(offerSqlRepository.deleteMainOfferData(any(UUID.class)))
                .thenReturn(10);

        long result = offerService.deleteOffersByInventoryId(inventoryId);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(10);

        verify(offerSqlRepository, times(1)).deleteCarloanPreviewByInventoryId(inventoryId);
        verify(offerSqlRepository, times(1)).deletePreordersCartByInventoryId(inventoryId);
        verify(offerSqlRepository, times(1)).deleteTinyLocatorsByInventoryId(inventoryId);
        verify(offerSqlRepository, times(1)).deleteResourcesByInventoryId(inventoryId);
        verify(offerSqlRepository, times(1)).deleteMainOfferData(inventoryId);
    }

    @Test
    @DisplayName("deleteOffersByInventoryIdExcludingIds: Cobertura del bloque catch y handleDeletionError")
    void deleteOffersByInventoryIdExcludingIds_Catch_Coverage() {
        // GIVEN
        UUID inventoryId = UUID.randomUUID();
        List<UUID> idsToKeep = List.of(UUID.randomUUID());

        // Forzamos que la primera llamada del bloque try lance una excepción
        doThrow(new RuntimeException("SQL Execution Error"))
                .when(offerSqlRepository).deleteCarloanPreviewByInventoryExcluding(any(), any());

        // WHEN & THEN
        // Verificamos que se lanza la excepción de dominio envuelta por el handler
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Database error during offer deletion");

        // Verificamos que el log de error en handleDeletionError fue invocado (opcional)
        verify(offerSqlRepository, times(1)).deleteCarloanPreviewByInventoryExcluding(any(), any());
    }

    @Test
    @DisplayName("deleteByReferences: Debe ejecutar todos los borrados por referencia correctamente")
    void deleteOffersByInventoryIdAndReferences_Success_Coverage() {
        UUID inventoryId = UUID.randomUUID();
        List<String> externalReferences = List.of("REF1", "REF2");

        when(offerSqlRepository.deleteMainOfferDataByRefs(eq(inventoryId), eq(externalReferences)))
                .thenReturn(3);

        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, externalReferences);

        assertThat(result).isEqualTo(3);

        verify(offerSqlRepository).deleteCarloanPreviewByReferences(inventoryId, externalReferences);
        verify(offerSqlRepository).deletePreordersCartByReferences(inventoryId, externalReferences);
        verify(offerSqlRepository).deleteTinyLocatorsByReferences(inventoryId, externalReferences);
        verify(offerSqlRepository).deleteResourcesByReferences(inventoryId, externalReferences);
        verify(offerSqlRepository).deleteMainOfferDataByRefs(inventoryId, externalReferences);
    }

    @Test
    @DisplayName("deleteByReferences: Debe capturar error y ejecutar handleDeletionError")
    void deleteOffersByInventoryIdAndReferences_Catch_Coverage() {
        UUID inventoryId = UUID.randomUUID();
        List<String> refs = List.of("REF-ERROR");

        doThrow(new RuntimeException("Database error on references"))
                .when(offerSqlRepository).deleteCarloanPreviewByReferences(any(), any());

        assertThatThrownBy(() ->
                offerService.deleteOffersByInventoryIdAndReferences(inventoryId, refs))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE);

        verify(offerSqlRepository).deleteCarloanPreviewByReferences(eq(inventoryId), eq(refs));
    }

    @Test
    @DisplayName("findByExternalIdentities: Debe loguear advertencia y retornar vacío si externalIdInfo es null")
    void findByExternalIdentities_NullInfo_Coverage() {
        UUID inventoryId = UUID.randomUUID();

        OfferDto offerWithNullInfo = new OfferDto();
        offerWithNullInfo.setId(UUID.randomUUID());
        offerWithNullInfo.setInventoryId(inventoryId);
        offerWithNullInfo.setExternalIdInfo(null);

        VehicleInstanceDto vehicleInstance = new VehicleInstanceDto();
        vehicleInstance.setVehicleModel(new VehicleModelDto());
        offerWithNullInfo.setVehicleInstance(vehicleInstance);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(new OfferEntity());

        offerService.processOffer(offerWithNullInfo);

        verify(offerSqlRepository, never()).findByInventoryIdAndReferences(any(), any(), any(), any());

        verify(offerSqlRepository).saveAndFlush(any());
    }

    @Test
    @DisplayName("processOffer: Debe usar lastUpdated de la entidad existente para comparar fechas")
    void processOffer_UseExistingLastUpdated_Coverage() {
        OffsetDateTime incomingDate = OffsetDateTime.now();
        OffsetDateTime existingLastUpdated = incomingDate.minusDays(1);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setLastUpdated(existingLastUpdated);
        existingEntity.setCreatedAt(existingLastUpdated.minusDays(5));
        existingEntity.setVehicleInstance(new VehicleInstanceEntity());

        when(offerSqlRepository.findByInventoryIdAndReferences(any(UUID.class), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existingEntity));

        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        offerService.processOffer(sampleOfferDto);

        verify(offerSqlRepository).save(existingEntity);
        assertThat(existingEntity.getLastUpdated()).isEqualTo(incomingDate);
    }
}
