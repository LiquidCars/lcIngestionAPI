package net.liquidcars.ingestion.infra.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
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
        // GIVEN
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.empty());

        // --- ARREGLO AQUÍ ---
        // Simulamos que el mapper devuelve una entidad de dirección para evitar el NPE
        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());

        OfferEntity entityToSave = new OfferEntity();
        entityToSave.setVehicleInstance(new VehicleInstanceEntity());
        when(mapper.toEntity(any())).thenReturn(entityToSave);
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(entityToSave);

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        verify(offerSqlRepository).saveAndFlush(any(OfferEntity.class));
    }

    @Test
    @DisplayName("Should update offer only if incoming date is newer")
    void processOffer_ExistingOffer_NewerDate_ShouldUpdate() {
        // GIVEN
        OffsetDateTime oldDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime newDate = OffsetDateTime.now();

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setCreatedAt(oldDate);
        existingEntity.setVehicleInstance(new VehicleInstanceEntity());

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existingEntity));
        when(mapper.mapEpoch(anyLong())).thenReturn(newDate);

        // --- ARREGLO AQUÍ ---
        // En la actualización también se puede crear una dirección si no existía
        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        verify(offerSqlRepository).save(existingEntity);
    }

    @Test
    @DisplayName("Should NOT update offer if incoming date is older")
    void processOffer_ExistingOffer_OlderDate_ShouldDoNothing() {
        // GIVEN
        OffsetDateTime oldDate = OffsetDateTime.now().plusDays(1);
        OffsetDateTime newDate = OffsetDateTime.now();

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setCreatedAt(oldDate);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existingEntity));
        when(mapper.mapEpoch(anyLong())).thenReturn(newDate);

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        verify(offerSqlRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteByInventoryId should return count and log success")
    void deleteOffersByInventoryId_Success() {
        // GIVEN
        when(offerSqlRepository.deleteByInventoryId(inventoryId)).thenReturn(5L);

        // WHEN
        long result = offerService.deleteOffersByInventoryId(inventoryId);

        // THEN
        assertThat(result).isEqualTo(5L);
        verify(offerSqlRepository).deleteByInventoryId(inventoryId);
    }

    @Test
    @DisplayName("deleteByInventoryId should throw LCIngestionException on DB error")
    void deleteOffersByInventoryId_ThrowsException() {
        // GIVEN
        when(offerSqlRepository.deleteByInventoryId(inventoryId)).thenThrow(new RuntimeException("DB Down"));

        // WHEN & THEN
        assertThatThrownBy(() -> offerService.deleteOffersByInventoryId(inventoryId))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("SQL deletion error");
    }

    @Test
    @DisplayName("deleteExcludingIds should call deleteByInventoryId if list is empty")
    void deleteOffersExcludingIds_EmptyList_CallsDeleteAll() {
        // GIVEN
        when(offerSqlRepository.deleteByInventoryId(inventoryId)).thenReturn(10L);

        // WHEN
        long result = offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, Collections.emptyList());

        // THEN
        assertThat(result).isEqualTo(10L);
        verify(offerSqlRepository).deleteByInventoryId(inventoryId);
    }

    @Test
    @DisplayName("deleteByReferences should return 0 if references list is empty")
    void deleteByReferences_EmptyList_ReturnsZero() {
        // WHEN
        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, Collections.emptyList());

        // THEN
        assertThat(result).isZero();
        verifyNoInteractions(offerSqlRepository);
    }

    @Test
    @DisplayName("deleteExcludingIds should call specific repository method when list has IDs")
    void deleteOffersExcludingIds_WithList_CallsCorrectRepository() {
        List<UUID> idsToKeep = List.of(UUID.randomUUID());
        when(offerSqlRepository.deleteByInventoryIdAndIdNotIn(inventoryId, idsToKeep)).thenReturn(3L);

        long result = offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep);

        assertThat(result).isEqualTo(3L);
        verify(offerSqlRepository).deleteByInventoryIdAndIdNotIn(inventoryId, idsToKeep);
    }

    @Test
    @DisplayName("deleteByReferences should call repository when list is NOT empty")
    void deleteByReferences_WithList_CallsRepository() {
        // GIVEN
        List<String> refs = List.of("REF-123");
        when(offerSqlRepository.deleteByInventoryIdAndReferencesIn(inventoryId, refs)).thenReturn(2L);

        // WHEN
        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, refs);

        // THEN
        assertThat(result).isEqualTo(2L);
        verify(offerSqlRepository).deleteByInventoryIdAndReferencesIn(inventoryId, refs);
    }

    @Test
    @DisplayName("updateFullOffer should create new address if it doesn't exist in DB")
    void updateFullOffer_AddressNotFound_ShouldCreateNew() {
        // GIVEN
        OffsetDateTime incomingDate = OffsetDateTime.now();
        OffsetDateTime existingDate = incomingDate.minusDays(1);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setCreatedAt(existingDate); // Para que existingDate no sea null
        existingEntity.setVehicleInstance(new VehicleInstanceEntity());

        // 1. Mock de Repositorios
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existingEntity));
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        // Forzamos que la dirección NO exista en el repositorio para entrar en la rama de creación
        when(participantAddressEntityRepository.findById(any())).thenReturn(Optional.empty());

        // 2. Mock de Mapper (Crucial para evitar NPE)
        // Configuramos incomingDate para que isAfter() no falle
        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);
        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        // Verificamos que se intentó guardar la nueva dirección
        verify(participantAddressEntityRepository).save(any(ParticipantAddressEntity.class));
        // Verificamos que se guardó la oferta actualizada
        verify(offerSqlRepository).save(existingEntity);
    }

    @Test
    @DisplayName("processOffer should create new VehicleModel if not found in DB")
    void processOffer_ModelNotFound_ShouldCreateNewModel() {
        // GIVEN
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.empty());

        // Configuramos el mock para que no devuelva nulo en la dirección
        when(mapper.toParticipantAddressEntity(any())).thenReturn(new ParticipantAddressEntity());

        // Simular que el modelo NO existe
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.empty());

        // Entidades necesarias para que el flujo de creación no rompa
        VehicleModelEntity newModel = new VehicleModelEntity();
        when(mapper.toVehicleModelEntity(any())).thenReturn(newModel);
        when(vehicleModelRepository.save(any(VehicleModelEntity.class))).thenReturn(newModel);

        OfferEntity offerEntity = new OfferEntity();
        offerEntity.setVehicleInstance(new VehicleInstanceEntity());
        when(mapper.toEntity(any())).thenReturn(offerEntity);
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(offerEntity);

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        verify(vehicleModelRepository).save(any(VehicleModelEntity.class));
        verify(offerSqlRepository).saveAndFlush(any(OfferEntity.class));
    }

    @Test
    @DisplayName("deleteExcludingIds should call repository when list is NOT empty")
    void deleteOffersExcludingIds_WithList_CallsRepository() {
        // GIVEN
        List<UUID> idsToKeep = List.of(UUID.randomUUID());
        when(offerSqlRepository.deleteByInventoryIdAndIdNotIn(inventoryId, idsToKeep)).thenReturn(5L);

        // WHEN
        long result = offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep);

        // THEN
        assertThat(result).isEqualTo(5L);
        verify(offerSqlRepository).deleteByInventoryIdAndIdNotIn(inventoryId, idsToKeep);
    }

    @Test
    @DisplayName("processOffer should handle equipments with null type")
    void processOffer_Equipments_ShouldSetDefaultTypeIfNull() {
        // GIVEN
        OfferEntity offerEntity = new OfferEntity();
        VehicleInstanceEntity vehicleInstance = new VehicleInstanceEntity();
        vehicleInstance.setId(123L);
        offerEntity.setVehicleInstance(vehicleInstance);

        // Setup de mocks de repositorios
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(offerEntity);
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(offerEntity);
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        // Simulamos que el mapper devuelve un equipo sin tipo
        net.liquidcars.ingestion.infra.postgresql.entity.CarInstanceEquipmentEntity equipment =
                new net.liquidcars.ingestion.infra.postgresql.entity.CarInstanceEquipmentEntity();
        equipment.setType(null);

        when(mapper.toCarInstanceEquipmentEntityList(any())).thenReturn(List.of(equipment));

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        verify(carInstanceEquipmentEntityRepository).saveAll(anyList());
        assertThat(equipment.getType()).isNotNull();
        assertThat(equipment.getType().getId()).isEqualTo("Other");
        // Verificamos que se asignó el vehículo al equipo
        assertThat(equipment.getVehicleInstance()).isEqualTo(vehicleInstance);
    }

    @Test
    @DisplayName("updateFullOffer should update existing address (covers lambda$updateFullOffer$4)")
    void updateFullOffer_AddressExists_ShouldUpdate() {
        // GIVEN
        OffsetDateTime incomingDate = OffsetDateTime.now();
        OfferEntity existingOffer = new OfferEntity();
        existingOffer.setCreatedAt(incomingDate.minusDays(1));
        existingOffer.setVehicleInstance(new VehicleInstanceEntity());

        ParticipantAddressEntity existingAddr = new ParticipantAddressEntity();

        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existingOffer));
        when(mapper.mapEpoch(anyLong())).thenReturn(incomingDate);
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        // Simulamos que la dirección SÍ existe en el repo
        when(participantAddressEntityRepository.findById(any())).thenReturn(Optional.of(existingAddr));

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        // Verificamos que se llamó al mapper para actualizar la dirección existente
        verify(mapper).updateAddressFromDto(eq(sampleOfferDto.getPickUpAddress()), eq(existingAddr));
        verify(participantAddressEntityRepository, never()).save(any(ParticipantAddressEntity.class));
    }

    @Test
    @DisplayName("processOffer should throw LCIngestionException when repository fails")
    void processOffer_ShouldThrowException_WhenRepositoryFails() {
        // GIVEN: Forzamos una excepción en el primer paso del método
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection error"));

        // WHEN & THEN
        assertThatThrownBy(() -> offerService.processOffer(sampleOfferDto))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("SQL persistence error");
    }

    @Test
    @DisplayName("convertUrlToBytes should return null if url is null")
    void convertUrlToBytes_ShouldReturnNull_WhenUrlIsNull() {
        // Para probar esto, necesitamos que el flujo de recursos reciba una URL nula
        // O simplemente llamar a una oferta que tenga un recurso con URL null
        sampleOfferDto.setResources(List.of(TestDataFactory.createCarOfferResourceDto()));

        // Necesitamos los mocks mínimos para que llegue hasta saveOrUpdateResources
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        when(offerSqlRepository.saveAndFlush(any())).thenReturn(new OfferEntity());
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN: El código pasará por convertUrlToBytes(null) y cubrirá la rama del 'return null'
        verify(carOfferResourceRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("deleteByReferences should throw LCIngestionException on error")
    void deleteByReferences_ShouldThrowException_OnError() {
        List<String> refs = List.of("REF1");
        when(offerSqlRepository.deleteByInventoryIdAndReferencesIn(any(), any()))
                .thenThrow(new RuntimeException("DB Timeout"));

        assertThatThrownBy(() -> offerService.deleteOffersByInventoryIdAndReferences(inventoryId, refs))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("by references");
    }

    @Test
    @DisplayName("deleteExcludingIds should throw LCIngestionException on error")
    void deleteExcludingIds_ShouldThrowException_OnError() {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(offerSqlRepository.deleteByInventoryIdAndIdNotIn(any(), any()))
                .thenThrow(new RuntimeException("DB Failure"));

        assertThatThrownBy(() -> offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, ids))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("excluding specific IDs");
    }

    @Test
    @DisplayName("updateFullOffer should skip vehicle logic if vehicleInstance is null")
    void updateFullOffer_ShouldHandleNullVehicleInstance() {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        OfferEntity existing = new OfferEntity();
        existing.setVehicleInstance(null); // Provocamos el caso de prueba
        existing.setCreatedAt(now.minusDays(1));

        // Aseguramos que el DTO tenga equipos para que intente entrar en la lógica
        sampleOfferDto.getVehicleInstance().setEquipments(List.of(new net.liquidcars.ingestion.domain.model.CarInstanceEquipmentDto()));

        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existing));
        when(mapper.mapEpoch(anyLong())).thenReturn(now);
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        verify(offerSqlRepository).save(existing);
        // Verificamos que NO se llamó al borrado de equipos porque el vehículo era null
        verify(carInstanceEquipmentEntityRepository, never()).deleteByVehicleInstanceId(anyLong());
    }

    @Test
    @DisplayName("updateFullOffer should restore vehicle ID and update JSON offer text")
    void updateFullOffer_ShouldRestoreIdAndUpdateJson() {
        // GIVEN
        Long expectedVehicleId = 999L;
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Entidad existente con objetos internos instanciados para entrar en los IF
        OfferEntity existing = new OfferEntity();
        existing.setCreatedAt(now.minusDays(1));

        VehicleInstanceEntity existingVehicle = new VehicleInstanceEntity();
        existingVehicle.setId(expectedVehicleId);
        existing.setVehicleInstance(existingVehicle);

        JsonOfferEntity existingJson = new JsonOfferEntity();
        existing.setJsonCarOffer(existingJson);

        // 2. Mocks de Repositorios y Mapper
        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existing));
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(mapper.mapEpoch(anyLong())).thenReturn(now);

        // 3. Mock del ObjectMapper (Arreglo para IllegalArgumentException)
        Map<String, Object> mockJsonMap = new java.util.HashMap<>();
        mockJsonMap.put("key", "value");

        // Usamos este matcher para que Jackson no intente introspección sobre el mock
        doReturn(mockJsonMap).when(objectMapper).convertValue(any(), eq(Map.class));

        // 4. Mock de dirección para evitar NPE en el flujo de actualización
        lenient().when(participantAddressEntityRepository.findById(any()))
                .thenReturn(Optional.of(new ParticipantAddressEntity()));

        // WHEN
        offerService.processOffer(sampleOfferDto);

        // THEN
        // Verificamos restauración de ID (Línea: existing.getVehicleInstance().setId(existingVehicleInstanceId))
        assertThat(existing.getVehicleInstance().getId()).isEqualTo(expectedVehicleId);

        // Verificamos actualización JSON (Bloque: if (existing.getJsonCarOffer() != null))
        assertThat(existing.getJsonCarOffer().getTexto()).isEqualTo(mockJsonMap);
        assertThat(existing.getJsonCarOffer().getCreatedAt()).isNotNull();

        verify(offerSqlRepository).save(existing);
    }

    @Test
    @DisplayName("updateFullOffer should generate random ID if existing vehicle instance ID is null")
    void updateFullOffer_ShouldGenerateRandomId_WhenExistingIdIsNull() {
        OfferEntity existing = new OfferEntity();
        existing.setCreatedAt(OffsetDateTime.now().minusDays(1));
        existing.setVehicleInstance(new VehicleInstanceEntity()); // ID es null por defecto

        when(offerSqlRepository.findByHash(anyInt())).thenReturn(Optional.of(existing));
        when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now());
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));

        offerService.processOffer(sampleOfferDto);

        assertThat(existing.getVehicleInstance().getId()).isNotNull();
        assertThat(existing.getVehicleInstance().getId()).isBetween(100_000_000L, 999_999_999L);
    }
}
