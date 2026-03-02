package net.liquidcars.ingestion.infra.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.liquidcars.ingestion.domain.model.*;
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
    @Mock private  VehicleInstanceRepository vehicleInstanceRepository;
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
        UUID inventoryId = UUID.randomUUID();
        List<UUID> idsToKeep = List.of(UUID.randomUUID());

        doThrow(new RuntimeException("SQL Execution Error"))
                .when(offerSqlRepository).deleteCarloanPreviewByInventoryExcluding(any(), any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Database error during offer deletion");

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

    @Test
    @DisplayName("findExternalRefsByOfferIds: Cobertura de flattening de referencias")
    void findExternalRefsByOfferIds_Coverage() {
        UUID id = UUID.randomUUID();
        ExternalIdInfoProjection proj = mock(ExternalIdInfoProjection.class);
        when(proj.getOwnerReference()).thenReturn("OWN");
        when(proj.getDealerReference()).thenReturn("DLR");
        when(proj.getChannelReference()).thenReturn(null);

        when(offerSqlRepository.findExternalRefsByOfferIds(anyList())).thenReturn(List.of(proj));

        Set<String> refs = offerService.findExternalRefsByOfferIds(List.of(id));

        assertThat(refs).containsExactlyInAnyOrder("OWN", "DLR");
    }

    @Test
    @DisplayName("deleteOffersByInventoryIdAndReferences: Cobertura de retorno temprano")
    void deleteOffersByReferences_Empty_Coverage() {
        long result = offerService.deleteOffersByInventoryIdAndReferences(UUID.randomUUID(), List.of());
        assertThat(result).isZero();
        verifyNoInteractions(offerSqlRepository);
    }

    @Test
    @DisplayName("extractRef: Cobertura de casos null")
    void extractRef_Null_Coverage() {
        String result = (String) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                offerService, "extractRef", (Object) null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("processBatch: Should return empty list when offers list is empty")
    void processBatch_EmptyOffers_ReturnsEmptyList() {
        List<UUID> result = offerService.processBatch(List.of(), List.of());
        assertThat(result).isEmpty();
        verifyNoInteractions(offerSqlRepository);
    }

    @Test
    @DisplayName("processBatch: Should insert new offer when no existing entity found")
    void processBatch_NewOffer_ShouldInsert() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        VehicleModelEntity model = new VehicleModelEntity();
        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(100_000_001L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(model));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));

        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(eq(inventoryId), anyList()))
                .thenReturn(List.of());

        OfferEntity newEntity = new OfferEntity();
        newEntity.setVehicleInstance(instance);
        when(mapper.toEntity(any())).thenReturn(newEntity);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of(newEntity));

        List<UUID> result = offerService.processBatch(List.of(offer), List.of());

        assertThat(result).contains(offer.getId());
        verify(offerSqlRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    @DisplayName("processBatch: Should update existing offer when incoming date is newer")
    void processBatch_ExistingOffer_NewerDate_ShouldUpdate() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime newDate = OffsetDateTime.now();

        OfferEntity existing = new OfferEntity();
        existing.setId(UUID.randomUUID());
        existing.setCreatedAt(oldDate);
        existing.setOwnerReference(offer.getExternalIdInfo().getOwnerReference());
        existing.setDealerReference(offer.getExternalIdInfo().getDealerReference());
        existing.setChannelReference(offer.getExternalIdInfo().getChannelReference());
        VehicleInstanceEntity existingVehicle = new VehicleInstanceEntity();
        existingVehicle.setId(555L);
        existing.setVehicleInstance(existingVehicle);

        VehicleModelEntity model = new VehicleModelEntity();
        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(555L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(model));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));

        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(eq(inventoryId), anyList()))
                .thenReturn(List.of(existing));

        when(mapper.mapEpoch(anyLong())).thenReturn(newDate);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of());

        List<UUID> result = offerService.processBatch(List.of(offer), List.of());

        assertThat(result).contains(existing.getId());
        verify(mapper).updateEntityFromDto(eq(offer), eq(existing));
    }

    @Test
    @DisplayName("processBatch: Should NOT update existing offer when incoming date is older")
    void processBatch_ExistingOffer_OlderDate_ShouldSkipUpdate() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        OffsetDateTime futureDate = OffsetDateTime.now().plusDays(1);
        OffsetDateTime incomingDate = OffsetDateTime.now();

        OfferEntity existing = new OfferEntity();
        existing.setId(UUID.randomUUID());
        existing.setCreatedAt(futureDate);
        existing.setOwnerReference(offer.getExternalIdInfo().getOwnerReference());
        existing.setDealerReference(offer.getExternalIdInfo().getDealerReference());
        existing.setChannelReference(offer.getExternalIdInfo().getChannelReference());
        existing.setVehicleInstance(new VehicleInstanceEntity());

        VehicleModelEntity model = new VehicleModelEntity();
        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(100_000_001L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(model));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(eq(inventoryId), anyList()))
                .thenReturn(List.of(existing));
        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of());

        offerService.processBatch(List.of(offer), List.of());

        verify(mapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    @DisplayName("processBatch: Cobertura total de lambdas de caché y resolución de colisiones")
    void processBatch_LambdasAndCollision_Coverage() {
        KeyValueDto<String, String> safeKV = new KeyValueDto<>("KEY", "VALUE");
        OfferDto offerDto = TestDataFactory.createOfferDto();

        offerDto.setParticipantId(UUID.randomUUID());
        offerDto.setLastUpdated(System.currentTimeMillis());
        offerDto.setResources(List.of(TestDataFactory.createCarOfferResourceDto()));
        offerDto.getVehicleInstance().setEquipments(List.of(new CarInstanceEquipmentDto()));

        VehicleModelDto modelDto = offerDto.getVehicleInstance().getVehicleModel();
        modelDto.setBodyType(safeKV);
        modelDto.setChangeType(safeKV);
        modelDto.setFuelType(safeKV);

        OfferEntity mockedOfferEntity = new OfferEntity();
        mockedOfferEntity.setId(UUID.randomUUID());
        VehicleInstanceEntity mockedInstanceEntity = new VehicleInstanceEntity();
        mockedInstanceEntity.setId(999L);
        mockedOfferEntity.setVehicleInstance(mockedInstanceEntity);

        OfferEntity existing1 = new OfferEntity();
        existing1.setId(UUID.randomUUID());
        existing1.setOwnerReference("REF");
        existing1.setDealerReference("DLR");
        existing1.setChannelReference("CH");
        existing1.setCreatedAt(OffsetDateTime.now().minusDays(1));

        OfferEntity existing2 = new OfferEntity();
        existing2.setId(UUID.randomUUID());
        existing2.setOwnerReference("REF");
        existing2.setDealerReference("DLR");
        existing2.setChannelReference("CH");

        when(mapper.toEntity(any())).thenReturn(mockedOfferEntity);

        lenient().when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());
        lenient().when(mapper.toCarInstanceEquipmentEntityList(any())).thenReturn(List.of(new CarInstanceEquipmentEntity()));

        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(any(), anyList()))
                .thenReturn(List.of(existing1, existing2));

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(mockedInstanceEntity));

        when(offerSqlRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        offerService.processBatch(List.of(offerDto), List.of());

        verify(offerSqlRepository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    @DisplayName("processBatch: Should throw LCIngestionException when a fatal error occurs")
    void processBatch_ShouldThrowException_WhenFatalError() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> offerService.processBatch(List.of(offer), List.of()))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Error processing SQL batch");
    }

    @Test
    @DisplayName("processBatch: Should update JSON offer if existing jsonCarOffer is not null")
    void processBatch_ShouldUpdateJson_WhenJsonCarOfferExists() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime newDate = OffsetDateTime.now();

        JsonOfferEntity existingJson = new JsonOfferEntity();
        OfferEntity existing = new OfferEntity();
        existing.setId(UUID.randomUUID());
        existing.setCreatedAt(oldDate);
        existing.setOwnerReference(offer.getExternalIdInfo().getOwnerReference());
        existing.setDealerReference(offer.getExternalIdInfo().getDealerReference());
        existing.setChannelReference(offer.getExternalIdInfo().getChannelReference());
        existing.setVehicleInstance(new VehicleInstanceEntity());
        existing.setJsonCarOffer(existingJson);

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(100_000_001L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(eq(inventoryId), anyList()))
                .thenReturn(List.of(existing));
        when(mapper.mapEpoch(anyLong())).thenReturn(newDate);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of());

        Map<String, Object> jsonMap = Map.of("field", "value");
        doReturn(jsonMap).when(objectMapper).convertValue(any(), eq(Map.class));

        offerService.processBatch(List.of(offer), List.of());

        assertThat(existingJson.getTexto()).isEqualTo(jsonMap);
        assertThat(existingJson.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("processBatch: New offer with null vehicleInstance ID should generate random ID")
    void processBatch_NewOffer_ShouldGenerateRandomVehicleInstanceId() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(null);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(eq(inventoryId), anyList()))
                .thenReturn(List.of());

        OfferEntity newEntity = new OfferEntity();
        newEntity.setVehicleInstance(instance);
        when(mapper.toEntity(any())).thenReturn(newEntity);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of(newEntity));

        offerService.processBatch(List.of(offer), List.of());

        assertThat(newEntity.getVehicleInstance().getId())
                .isNotNull()
                .isBetween(100_000_000L, 999_999_999L);
    }

    @Test
    @DisplayName("ensureVehicleInstanceExists: Should return existing instance if found")
    void ensureVehicleInstanceExists_Found_ShouldReturnExisting() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        VehicleInstanceEntity existingInstance = new VehicleInstanceEntity();
        existingInstance.setId(777L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(existingInstance));
        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(any(), anyList())).thenReturn(List.of());

        OfferEntity newEntity = new OfferEntity();
        newEntity.setVehicleInstance(existingInstance);
        when(mapper.toEntity(any())).thenReturn(newEntity);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of(newEntity));

        offerService.processBatch(List.of(offer), List.of());

        verify(vehicleInstanceRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("ensureVehicleInstanceExists: Should create and save instance if not found")
    void ensureVehicleInstanceExists_NotFound_ShouldCreate() {
        OfferDto offer = OfferDtoFactory.getOfferDto();
        offer.setInventoryId(inventoryId);

        VehicleInstanceEntity newInstance = new VehicleInstanceEntity();

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(mapper.toVehicleInstanceEntity(any())).thenReturn(newInstance);
        when(vehicleInstanceRepository.saveAndFlush(any())).thenReturn(newInstance);

        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(any(), anyList())).thenReturn(List.of());
        OfferEntity newEntity = new OfferEntity();
        newEntity.setVehicleInstance(newInstance);
        when(mapper.toEntity(any())).thenReturn(newEntity);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of(newEntity));

        offerService.processBatch(List.of(offer), List.of());

        verify(vehicleInstanceRepository).saveAndFlush(any(VehicleInstanceEntity.class));
        assertThat(newInstance.isEnabled()).isTrue();
        assertThat(newInstance.getId()).isBetween(100_000_000L, 999_999_999L);
    }

    @Test
    @DisplayName("modelKeyModel: Should produce lowercase pipe-separated key")
    void modelKeyModel_ShouldReturnCorrectKey() {
        VehicleModelDto dto = new VehicleModelDto();
        dto.setBrand("Toyota");
        dto.setModel("Corolla");
        dto.setVersion("1.8 Hybrid");

        String key = (String) org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(offerService, "modelKeyModel", dto);

        assertThat(key).isEqualTo("toyota|corolla|1.8 hybrid");
    }

    @Test
    @DisplayName("modelKeyInstance: Should produce lowercase pipe-separated key from plate and chassis")
    void modelKeyInstance_ShouldReturnCorrectKey() {
        VehicleInstanceDto dto = new VehicleInstanceDto();
        dto.setPlate("AB1234CD");
        dto.setChassisNumber("VIN123456789");

        String key = (String) org.springframework.test.util.ReflectionTestUtils
                .invokeMethod(offerService, "modelKeyInstance", dto);

        assertThat(key).isEqualTo("ab1234cd|vin123456789");
    }

    @Test
    @DisplayName("buildModelCache: Should only call ensureVehicleModelExists once per unique model key")
    void buildModelCache_ShouldDeduplicateModels() {
        VehicleModelDto sharedModel = new VehicleModelDto();
        sharedModel.setBrand("BMW");
        sharedModel.setModel("Serie 3");
        sharedModel.setVersion("320d");

        OfferDto offer1 = OfferDtoFactory.getOfferDto();
        offer1.getVehicleInstance().setVehicleModel(sharedModel);
        offer1.setInventoryId(inventoryId);

        OfferDto offer2 = OfferDtoFactory.getOfferDto();
        offer2.getVehicleInstance().setVehicleModel(sharedModel);
        offer2.setInventoryId(inventoryId);

        VehicleModelEntity modelEntity = new VehicleModelEntity();
        VehicleInstanceEntity instanceEntity = new VehicleInstanceEntity();
        instanceEntity.setId(100_000_001L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(
                eq("BMW"), eq("Serie 3"), eq("320d")))
                .thenReturn(Optional.of(modelEntity));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instanceEntity));
        when(offerSqlRepository.findByInventoryIdAndOwnerRefs(any(), anyList())).thenReturn(List.of());

        OfferEntity e1 = new OfferEntity();
        e1.setVehicleInstance(instanceEntity);
        OfferEntity e2 = new OfferEntity();
        e2.setVehicleInstance(instanceEntity);
        when(mapper.toEntity(any())).thenReturn(e1).thenReturn(e2);
        when(offerSqlRepository.saveAll(anyList())).thenReturn(List.of(e1, e2));

        offerService.processBatch(List.of(offer1, offer2), List.of());

        verify(vehicleModelRepository, times(1))
                .findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase("BMW", "Serie 3", "320d");
    }

    @Test
    @DisplayName("findActiveBookedOfferIds: Debe retornar la lista de IDs cuando la consulta es exitosa")
    void findActiveBookedOfferIds_Success_Coverage() {
        UUID inventoryId = UUID.randomUUID();
        List<UUID> expectedIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        when(offerSqlRepository.findActiveBookedOfferIds(inventoryId))
                .thenReturn(expectedIds);

        List<UUID> result = offerService.findActiveBookedOfferIds(inventoryId);

        assertThat(result).hasSize(2).containsAll(expectedIds);
        verify(offerSqlRepository).findActiveBookedOfferIds(inventoryId);
    }

    @Test
    @DisplayName("findActiveBookedOfferIds: Debe lanzar LCIngestionException si el repositorio falla")
    void findActiveBookedOfferIds_Exception_Coverage() {
        UUID inventoryId = UUID.randomUUID();
        when(offerSqlRepository.findActiveBookedOfferIds(inventoryId))
                .thenThrow(new RuntimeException("DB Connection Timeout"));

        assertThatThrownBy(() -> offerService.findActiveBookedOfferIds(inventoryId))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error finding active bookings for inventoryId")
                .extracting("techCause")
                .isEqualTo(LCTechCauseEnum.DATABASE);
    }

    @Test
    @DisplayName("findExternalRefsByOfferIds: Debe capturar error del repo y relanzar LCIngestionException")
    void findExternalRefsByOfferIds_ShouldThrowExceptionOnRepositoryFailure() {
        List<UUID> offerIds = List.of(UUID.randomUUID());

        when(offerSqlRepository.findExternalRefsByOfferIds(anyList()))
                .thenThrow(new RuntimeException("Data access error"));

        assertThatThrownBy(() -> offerService.findExternalRefsByOfferIds(offerIds))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error finding external references for offer ids:")
                .satisfies(ex -> {
                    LCIngestionException lce = (LCIngestionException) ex;
                    assertThat(lce.getTechCause()).isEqualTo(LCTechCauseEnum.DATABASE);
                    assertThat(lce.getCause()).isInstanceOf(RuntimeException.class);
                });

        verify(offerSqlRepository).findExternalRefsByOfferIds(offerIds);
    }
}
