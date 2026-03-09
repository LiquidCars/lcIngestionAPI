package net.liquidcars.ingestion.infra.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.liquidcars.ingestion.domain.model.AgreementDto;
import net.liquidcars.ingestion.domain.model.CarOfferResourceDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.VehicleOfferDto;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.liquidcars.ingestion.domain.service.utils.OfferUtils.extractRef;
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
    @Mock private VehicleInstanceRepository vehicleInstanceRepository;
    @Mock private OfferInfraSQLMapper mapper;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private TinyLocatorRepository tinyLocatorRepository;
    @Mock private AgreementRepository agreementRepository;

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

    // ─────────────────────────────────────────────────────────────────────────
    // deleteOffersByInventoryId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOffersByInventoryId: Debe cubrir todas las llamadas secuenciales de borrado")
    void deleteOffersByInventoryId_FullCoverage() {
        when(offerSqlRepository.deleteMainOfferData(any(UUID.class))).thenReturn(10);

        long result = offerService.deleteOffersByInventoryId(inventoryId);

        assertThat(result).isEqualTo(10);
        verify(offerSqlRepository).deleteCarloanPreviewByInventoryId(inventoryId);
        verify(offerSqlRepository).deletePreordersCartByInventoryId(inventoryId);
        verify(offerSqlRepository).deleteTinyLocatorsByInventoryId(inventoryId);
        verify(offerSqlRepository).deleteResourcesByInventoryId(inventoryId);
        verify(offerSqlRepository).deleteMainOfferData(inventoryId);
    }

    @Test
    @DisplayName("handleDeletionError: Debe capturar error de DB y lanzar LCIngestionException")
    void handleDeletionError_Coverage() {
        doThrow(new RuntimeException("Database connection lost"))
                .when(offerSqlRepository).deleteCarloanPreviewByInventoryId(any());

        assertThatThrownBy(() -> offerService.deleteOffersByInventoryId(inventoryId))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Database error during offer deletion");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteOffersByInventoryIdExcludingIds
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOffersByInventoryIdExcludingIds: Cobertura de borrado delta")
    void deleteOffersByInventoryIdExcludingIds_Success_Coverage() {
        List<UUID> idsToKeep = List.of(UUID.randomUUID());
        when(offerSqlRepository.deleteMainOfferDataExcluding(eq(inventoryId), anyList())).thenReturn(10);

        long result = offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep);

        assertThat(result).isEqualTo(10L);
        verify(offerSqlRepository).deleteCarloanPreviewByInventoryExcluding(inventoryId, idsToKeep);
        verify(offerSqlRepository).deletePreordersCartByInventoryExcluding(inventoryId, idsToKeep);
        verify(offerSqlRepository).deleteTinyLocatorsByInventoryExcluding(inventoryId, idsToKeep);
        verify(offerSqlRepository).deleteResourcesByInventoryExcluding(inventoryId, idsToKeep);
        verify(offerSqlRepository).deleteMainOfferDataExcluding(inventoryId, idsToKeep);
    }

    @Test
    @DisplayName("deleteOffersByInventoryIdExcludingIds: Lista vacía delega a deleteOffersByInventoryId")
    void deleteOffersByInventoryIdExcludingIds_EmptyList_DelegatesToFullDelete() {
        when(offerSqlRepository.deleteMainOfferData(inventoryId)).thenReturn(5);

        long result = offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, Collections.emptyList());

        assertThat(result).isEqualTo(5);
        verify(offerSqlRepository).deleteMainOfferData(inventoryId);
        verify(offerSqlRepository, never()).deleteMainOfferDataExcluding(any(), any());
    }

    @Test
    @DisplayName("deleteOffersByInventoryIdExcludingIds: Cobertura del bloque catch")
    void deleteOffersByInventoryIdExcludingIds_Catch_Coverage() {
        List<UUID> idsToKeep = List.of(UUID.randomUUID());
        doThrow(new RuntimeException("SQL Execution Error"))
                .when(offerSqlRepository).deleteCarloanPreviewByInventoryExcluding(any(), any());

        assertThatThrownBy(() -> offerService.deleteOffersByInventoryIdExcludingIds(inventoryId, idsToKeep))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Database error during offer deletion");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteOffersByInventoryIdAndReferences
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteByReferences: Retorna 0 si la lista de referencias está vacía")
    void deleteByReferences_EmptyList_ReturnsZero() {
        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, Collections.emptyList());

        assertThat(result).isZero();
        verifyNoInteractions(offerSqlRepository);
    }

    @Test
    @DisplayName("deleteByReferences: Ejecuta todos los borrados por referencia correctamente")
    void deleteOffersByInventoryIdAndReferences_Success_Coverage() {
        List<String> refs = List.of("REF1", "REF2");
        when(offerSqlRepository.deleteMainOfferDataByRefs(eq(inventoryId), eq(refs))).thenReturn(3);

        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, refs);

        assertThat(result).isEqualTo(3);
        verify(offerSqlRepository).deleteCarloanPreviewByReferences(inventoryId, refs);
        verify(offerSqlRepository).deletePreordersCartByReferences(inventoryId, refs);
        verify(offerSqlRepository).deleteTinyLocatorsByReferences(inventoryId, refs);
        verify(offerSqlRepository).deleteResourcesByReferences(inventoryId, refs);
        verify(offerSqlRepository).deleteMainOfferDataByRefs(inventoryId, refs);
    }

    @Test
    @DisplayName("deleteByReferences: Captura error y lanza LCIngestionException")
    void deleteOffersByInventoryIdAndReferences_Catch_Coverage() {
        List<String> refs = List.of("REF-ERROR");
        doThrow(new RuntimeException("Database error on references"))
                .when(offerSqlRepository).deleteCarloanPreviewByReferences(any(), any());

        assertThatThrownBy(() -> offerService.deleteOffersByInventoryIdAndReferences(inventoryId, refs))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findActiveBookedOfferIds
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findActiveBookedOfferIds: Retorna lista de IDs correctamente")
    void findActiveBookedOfferIds_Success() {
        List<UUID> expected = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(offerSqlRepository.findActiveBookedOfferIds(inventoryId)).thenReturn(expected);

        List<UUID> result = offerService.findActiveBookedOfferIds(inventoryId);

        assertThat(result).isEqualTo(expected);
        verify(offerSqlRepository).findActiveBookedOfferIds(inventoryId);
    }

    @Test
    @DisplayName("findActiveBookedOfferIds: Lanza LCIngestionException si ocurre error")
    void findActiveBookedOfferIds_ThrowsException() {
        when(offerSqlRepository.findActiveBookedOfferIds(inventoryId))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> offerService.findActiveBookedOfferIds(inventoryId))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Error finding active bookings");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findAgreementsByInventoryId
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAgreementsByInventoryId: Retorna lista de agreements correctamente")
    void findAgreementsByInventoryId_Success() {
        List<AgreementDto> expected = List.of(new AgreementDto());
        when(agreementRepository.findByInventoryId(inventoryId)).thenReturn(List.of(new AgreementEntity()));
        when(mapper.toAgreementDtoList(anyList())).thenReturn(expected);

        List<AgreementDto> result = offerService.findAgreementsByInventoryId(inventoryId);

        assertThat(result).isEqualTo(expected);
        verify(agreementRepository).findByInventoryId(inventoryId);
    }

    @Test
    @DisplayName("findAgreementsByInventoryId: Lanza LCIngestionException si ocurre error")
    void findAgreementsByInventoryId_ThrowsException() {
        when(agreementRepository.findByInventoryId(inventoryId))
                .thenThrow(new RuntimeException("DB connection error"));

        assertThatThrownBy(() -> offerService.findAgreementsByInventoryId(inventoryId))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Error finding agreements");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findExternalRefsByOfferIds
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findExternalRefsByOfferIds: Lista vacía retorna Set vacío sin consultar la BD")
    void findExternalRefsByOfferIds_EmptyList_ReturnsEmptySet() {
        var result = offerService.findExternalRefsByOfferIds(Collections.emptyList());

        assertThat(result).isEmpty();
        verifyNoInteractions(offerSqlRepository);
    }

    @Test
    @DisplayName("findExternalRefsByOfferIds: Retorna refs de owner, dealer y channel correctamente")
    void findExternalRefsByOfferIds_ReturnsAllRefs() {
        UUID offerId = UUID.randomUUID();
        ExternalIdInfoProjection projection = mock(ExternalIdInfoProjection.class);
        when(projection.getOwnerReference()).thenReturn("OWNER-1");
        when(projection.getDealerReference()).thenReturn("DEALER-1");
        when(projection.getChannelReference()).thenReturn("CHANNEL-1");
        when(offerSqlRepository.findExternalRefsByOfferIds(List.of(offerId))).thenReturn(List.of(projection));

        var result = offerService.findExternalRefsByOfferIds(List.of(offerId));

        assertThat(result).containsExactlyInAnyOrder("OWNER-1", "DEALER-1", "CHANNEL-1");
    }

    @Test
    @DisplayName("findExternalRefsByOfferIds: Filtra referencias nulas correctamente")
    void findExternalRefsByOfferIds_FiltersNullRefs() {
        UUID offerId = UUID.randomUUID();
        ExternalIdInfoProjection projection = mock(ExternalIdInfoProjection.class);
        when(projection.getOwnerReference()).thenReturn("OWNER-1");
        when(projection.getDealerReference()).thenReturn(null);
        when(projection.getChannelReference()).thenReturn(null);
        when(offerSqlRepository.findExternalRefsByOfferIds(anyList())).thenReturn(List.of(projection));

        var result = offerService.findExternalRefsByOfferIds(List.of(offerId));

        assertThat(result).containsExactly("OWNER-1");
    }

    @Test
    @DisplayName("findExternalRefsByOfferIds: Lanza LCIngestionException si ocurre error")
    void findExternalRefsByOfferIds_ThrowsException() {
        UUID offerId = UUID.randomUUID();
        when(offerSqlRepository.findExternalRefsByOfferIds(anyList()))
                .thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> offerService.findExternalRefsByOfferIds(List.of(offerId)))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Error finding external references");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — happy paths
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Retorna lista vacía si el batch está vacío")
    void processBatch_EmptyBatch_ReturnsEmptyList() {
        List<UUID> result = offerService.processBatch(Collections.emptyList(), Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("processBatch: Inserta nuevas ofertas y actualiza existentes correctamente")
    void processBatch_InsertAndUpdate_Success() {
        UUID newOfferId = UUID.randomUUID();
        UUID existingOfferId = UUID.randomUUID();

        VehicleOfferDto newOfferDto = TestDataFactory.createVehicleOfferDto(newOfferId, inventoryId, "REF-NEW");
        VehicleOfferDto updateOfferDto = TestDataFactory.createVehicleOfferDto(existingOfferId, inventoryId, "REF-UPDATE");
        updateOfferDto.setLastUpdated(System.currentTimeMillis());

        VehicleModelEntity modelEntity = new VehicleModelEntity();
        VehicleInstanceEntity instanceEntity = new VehicleInstanceEntity();
        instanceEntity.setId(100L);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setId(existingOfferId);
        existingEntity.setLastUpdated(OffsetDateTime.now().minusDays(1));
        existingEntity.setVehicleInstance(instanceEntity);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(modelEntity));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instanceEntity));
        when(offerSqlRepository.findExistingByAnyRef(eq(inventoryId), anyList(), anyList(), anyList()))
                .thenReturn(List.of(existingEntity));
        when(mapper.toEntity(any(VehicleOfferDto.class))).thenReturn(new OfferEntity());
        when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now());
        when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        doNothing().when(mapper).updateEntityFromDto(any(), any());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<UUID> result = offerService.processBatch(List.of(newOfferDto, updateOfferDto), Collections.emptyList());

        assertThat(result).hasSize(2).contains(newOfferId, existingOfferId);
        verify(offerSqlRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("processBatch: Booking Protection — no actualiza oferta con reserva activa")
    void processBatch_SkipUpdateIfBooked() {
        UUID bookedOfferId = UUID.randomUUID();
        VehicleOfferDto bookedDto = TestDataFactory.createVehicleOfferDto(bookedOfferId, inventoryId, "REF-BOOKED");
        bookedDto.getExternalIdInfo().setDealerReference(null);
        bookedDto.getExternalIdInfo().setChannelReference(null);

        String bookedRef = extractRef(bookedDto.getExternalIdInfo());

        OfferEntity existingBookedEntity = new OfferEntity();
        existingBookedEntity.setId(bookedOfferId);
        existingBookedEntity.setOwnerReference("REF-BOOKED");
        existingBookedEntity.setCreatedAt(OffsetDateTime.now().minusDays(1));

        lenient().when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now());
        lenient().when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        lenient().when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(new VehicleInstanceEntity()));

        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any()))
                .thenReturn(List.of(existingBookedEntity));

        ExternalIdInfoProjection projection = mock(ExternalIdInfoProjection.class);
        when(projection.getOwnerReference()).thenReturn(bookedRef);
        when(projection.getDealerReference()).thenReturn(null);
        when(projection.getChannelReference()).thenReturn(null);
        when(offerSqlRepository.findExternalRefsByOfferIds(anyList())).thenReturn(List.of(projection));

        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<UUID> result = offerService.processBatch(List.of(bookedDto), List.of(bookedOfferId));

        assertThat(result).containsExactly(bookedOfferId);
        verify(mapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    @DisplayName("processBatch: Ignora ofertas duplicadas dentro del mismo batch")
    void processBatch_IgnoreDuplicatesInSameBatch() {
        UUID duplicateId = UUID.randomUUID();
        VehicleOfferDto dto1 = TestDataFactory.createVehicleOfferDto(duplicateId, inventoryId, "REF-1");
        VehicleOfferDto dto2 = TestDataFactory.createVehicleOfferDto(duplicateId, inventoryId, "REF-1");

        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        lenient().when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(new VehicleInstanceEntity()));
        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<UUID> result = offerService.processBatch(List.of(dto1, dto2), List.of());

        assertThat(result).hasSize(2);
        ArgumentCaptor<List<OfferEntity>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(offerSqlRepository, atLeastOnce()).saveAll(listCaptor.capture());
        assertThat(listCaptor.getAllValues().get(0)).hasSize(1);
    }

    @Test
    @DisplayName("processBatch: Lanza LCIngestionException si ocurre error de base de datos")
    void processBatch_ThrowsExceptionOnSqlError() {
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(UUID.randomUUID(), inventoryId, "REF-1");
        lenient().when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        lenient().when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(new VehicleInstanceEntity()));
        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection Timeout"));

        assertThatThrownBy(() -> offerService.processBatch(List.of(dto), List.of()))
                .isInstanceOf(LCIngestionException.class)
                .hasFieldOrPropertyWithValue("techCause", LCTechCauseEnum.DATABASE)
                .hasMessageContaining("Error processing SQL batch");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — branches: ensureVehicleModel/Instance creation path
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Crea VehicleModel si no existe (lambda$ensureVehicleModelExists$0)")
    void processBatch_CreatesVehicleModelWhenNotFound() {
        UUID offerId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(offerId, inventoryId, "REF-NEW-MODEL");

        VehicleModelEntity newModel = new VehicleModelEntity();
        VehicleInstanceEntity instanceEntity = new VehicleInstanceEntity();
        instanceEntity.setId(1L);

        // Model NOT found → triggers the orElseGet lambda
        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(mapper.toVehicleModelEntity(any())).thenReturn(newModel);
        when(vehicleModelRepository.save(any())).thenReturn(newModel);

        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instanceEntity));

        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());
        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<UUID> result = offerService.processBatch(List.of(dto), List.of());

        assertThat(result).contains(offerId);
        verify(vehicleModelRepository).save(any());
    }

    @Test
    @DisplayName("processBatch: Crea VehicleInstance si no existe (lambda$ensureVehicleInstanceExists$1)")
    void processBatch_CreatesVehicleInstanceWhenNotFound() {
        UUID offerId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(offerId, inventoryId, "REF-NEW-INSTANCE");

        VehicleModelEntity modelEntity = new VehicleModelEntity();
        VehicleInstanceEntity newInstance = new VehicleInstanceEntity();
        newInstance.setId(99L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(modelEntity));

        // Instance NOT found → triggers the orElseGet lambda
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.empty());
        when(mapper.toVehicleInstanceEntity(any())).thenReturn(newInstance);
        when(vehicleInstanceRepository.saveAndFlush(any())).thenReturn(newInstance);

        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());
        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<UUID> result = offerService.processBatch(List.of(dto), List.of());

        assertThat(result).contains(offerId);
        verify(vehicleInstanceRepository).saveAndFlush(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — fallback by UUID (existing by id but not by ref)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Fallback a actualización por UUID cuando la ref no coincide")
    void processBatch_FallbackUpdateByUUID() {
        UUID existingId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(existingId, inventoryId, "REF-DIFFERENT");

        VehicleModelEntity modelEntity = new VehicleModelEntity();
        VehicleInstanceEntity instanceEntity = new VehicleInstanceEntity();
        instanceEntity.setId(50L);

        // Entity exists with same UUID but a different ref
        OfferEntity existingById = new OfferEntity();
        existingById.setId(existingId);
        existingById.setOwnerReference("OTHER-REF");
        existingById.setLastUpdated(OffsetDateTime.now().minusDays(1));
        existingById.setVehicleInstance(instanceEntity);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(modelEntity));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instanceEntity));

        // findExistingByAnyRef returns empty → no match by ref
        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());
        // findAllById returns entity with same UUID
        when(offerSqlRepository.findAllById(anyList())).thenReturn(List.of(existingById));

        when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        doNothing().when(mapper).updateEntityFromDto(any(), any());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<UUID> result = offerService.processBatch(List.of(dto), List.of());

        assertThat(result).containsExactly(existingId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — saveResourcesBatch: resources are non-null and URL is null
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Persiste recursos cuando el DTO tiene resources (lambda resource + convertUrlToBytes)")
    void processBatch_SavesResources_WithNullUrl() {
        UUID offerId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(offerId, inventoryId, "REF-RES");

        CarOfferResourceDto resourceWithNullUrl = new CarOfferResourceDto();
        resourceWithNullUrl.setResource(null);
        CarOfferResourceDto resourceWithUrl = new CarOfferResourceDto();
        resourceWithUrl.setResource("http://example.com/img.jpg");
        dto.setResources(List.of(resourceWithNullUrl, resourceWithUrl));

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(1L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());

        OfferEntity savedEntity = new OfferEntity();
        savedEntity.setId(offerId);
        when(mapper.toEntity(any())).thenReturn(savedEntity);
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> {
            List<OfferEntity> list = i.getArgument(0);
            list.forEach(e -> { if (e.getId() == null) e.setId(offerId); });
            return list;
        });

        offerService.processBatch(List.of(dto), List.of());

        verify(carOfferResourceRepository, atLeastOnce()).saveAll(anyList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — saveEquipmentsBatch: equipments with null vehicleInstance
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Persiste equipments y cubre lambda con tipo null (lambda$saveEquipmentsBatch$13)")
    void processBatch_SavesEquipments_WithNullType() {
        UUID offerId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(offerId, inventoryId, "REF-EQ");

        // Ensure the DTO has equipments
        dto.getVehicleInstance().setEquipments(List.of(new net.liquidcars.ingestion.domain.model.CarInstanceEquipmentDto()));

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(5L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());

        OfferEntity savedEntity = new OfferEntity();
        savedEntity.setId(offerId);
        savedEntity.setVehicleInstance(instance);
        when(mapper.toEntity(any())).thenReturn(savedEntity);
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());

        // Return equipment entity with null type to trigger the null-type branch
        CarInstanceEquipmentEntity eqWithNullType = new CarInstanceEquipmentEntity();
        eqWithNullType.setType(null);
        when(mapper.toCarInstanceEquipmentEntityList(anyList())).thenReturn(List.of(eqWithNullType));

        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> {
            List<OfferEntity> list = i.getArgument(0);
            list.forEach(e -> { if (e.getId() == null) e.setId(offerId); });
            return list;
        });

        offerService.processBatch(List.of(dto), List.of());

        verify(carInstanceEquipmentEntityRepository, atLeastOnce()).saveAll(anyList());
        // Verify the null type was defaulted to "Other"
        ArgumentCaptor<List<CarInstanceEquipmentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(carInstanceEquipmentEntityRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getType().getId()).isEqualTo("Other");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — loadExistingOffers: all references empty
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("loadExistingOffers: Cubre rama con todas las referencias vacías (sin consulta por ref)")
    void processBatch_LoadExistingOffers_AllRefsEmpty() {
        UUID offerId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(offerId, inventoryId, null);
        dto.getExternalIdInfo().setOwnerReference(null);
        dto.getExternalIdInfo().setDealerReference(null);
        dto.getExternalIdInfo().setChannelReference(null);

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(1L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));

        // No call to findExistingByAnyRef since all refs are null
        when(offerSqlRepository.findAllById(anyList())).thenReturn(List.of());
        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        offerService.processBatch(List.of(dto), List.of());

        verify(offerSqlRepository, never()).findExistingByAnyRef(any(), any(), any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — update path: no JSON / no VehicleInstance on existing entity
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Actualiza correctamente cuando jsonCarOffer y vehicleInstance son null en la entidad existente")
    void processBatch_Update_NullJsonAndNullVehicleInstance() {
        UUID existingId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(existingId, inventoryId, "REF-NULL-JSON");
        dto.setLastUpdated(System.currentTimeMillis());

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(10L);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setId(existingId);
        existingEntity.setLastUpdated(OffsetDateTime.now().minusDays(1));
        existingEntity.setVehicleInstance(null);   // null → skip vehicle update branch
        existingEntity.setJsonCarOffer(null);        // null → skip json update branch

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findExistingByAnyRef(eq(inventoryId), anyList(), anyList(), anyList()))
                .thenReturn(List.of(existingEntity));
        when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now());
        doNothing().when(mapper).updateEntityFromDto(any(), any());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<UUID> result = offerService.processBatch(List.of(dto), Collections.emptyList());

        assertThat(result).containsExactly(existingId);
        verify(mapper).updateEntityFromDto(eq(dto), eq(existingEntity));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — update path: incoming date NOT after existing date (skip)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: No actualiza si la fecha entrante no es posterior a la existente")
    void processBatch_Update_SkipsWhenDateNotNewer() {
        UUID existingId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(existingId, inventoryId, "REF-OLD-DATE");
        dto.setLastUpdated(1000L); // old epoch

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(10L);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setId(existingId);
        existingEntity.setLastUpdated(OffsetDateTime.now().plusDays(1)); // future → incoming is older

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findExistingByAnyRef(eq(inventoryId), anyList(), anyList(), anyList()))
                .thenReturn(List.of(existingEntity));
        when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now().minusDays(2)); // older than existing
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<UUID> result = offerService.processBatch(List.of(dto), Collections.emptyList());

        assertThat(result).containsExactly(existingId);
        verify(mapper, never()).updateEntityFromDto(any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — update path: existing.lastUpdated is null (falls back to createdAt)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Usa createdAt como fecha base cuando lastUpdated es null en entidad existente")
    void processBatch_Update_UsesCreatedAtWhenLastUpdatedNull() {
        UUID existingId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(existingId, inventoryId, "REF-CREATED");
        dto.setLastUpdated(System.currentTimeMillis());

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(10L);

        OfferEntity existingEntity = new OfferEntity();
        existingEntity.setId(existingId);
        existingEntity.setLastUpdated(null);                          // null → use createdAt
        existingEntity.setCreatedAt(OffsetDateTime.now().minusDays(2));
        existingEntity.setVehicleInstance(null);
        existingEntity.setJsonCarOffer(null);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findExistingByAnyRef(eq(inventoryId), anyList(), anyList(), anyList()))
                .thenReturn(List.of(existingEntity));
        when(mapper.mapEpoch(anyLong())).thenReturn(OffsetDateTime.now());
        doNothing().when(mapper).updateEntityFromDto(any(), any());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArguments()[0]);

        List<UUID> result = offerService.processBatch(List.of(dto), Collections.emptyList());

        assertThat(result).containsExactly(existingId);
        verify(mapper).updateEntityFromDto(any(), any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — saveAddressesBatch: participantId null branch
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: saveAddressesBatch no guarda si pickUpAddress o participantId son null")
    void processBatch_SaveAddresses_NullParticipantId() {
        UUID offerId = UUID.randomUUID();
        VehicleOfferDto dto = TestDataFactory.createVehicleOfferDto(offerId, inventoryId, "REF-ADDR");
        dto.setPickUpAddress(null); // null address → skip

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(1L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));
        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());
        when(mapper.toEntity(any())).thenReturn(new OfferEntity());
        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        offerService.processBatch(List.of(dto), List.of());

        verifyNoInteractions(participantAddressEntityRepository);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // processBatch — per-offer catch block
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("processBatch: Continúa con el batch si una oferta individual lanza excepción")
    void processBatch_ContinuesOnSingleOfferError() {
        UUID goodOfferId = UUID.randomUUID();
        UUID badOfferId  = UUID.randomUUID();

        VehicleOfferDto goodDto = TestDataFactory.createVehicleOfferDto(goodOfferId, inventoryId, "REF-GOOD");
        VehicleOfferDto badDto  = TestDataFactory.createVehicleOfferDto(badOfferId,  inventoryId, "REF-BAD");

        VehicleInstanceEntity instance = new VehicleInstanceEntity();
        instance.setId(1L);

        when(vehicleModelRepository.findFirstByBrandIgnoreCaseAndModelIgnoreCaseAndVersionIgnoreCase(any(), any(), any()))
                .thenReturn(Optional.of(new VehicleModelEntity()));
        when(vehicleInstanceRepository.findFirstByPlateIgnoreCaseAndChassisNumberIgnoreCase(any(), any()))
                .thenReturn(Optional.of(instance));

        when(offerSqlRepository.findExistingByAnyRef(any(), any(), any(), any())).thenReturn(List.of());

        when(mapper.toEntity(argThat((VehicleOfferDto d) -> d != null && badOfferId.equals(d.getId()))))
                .thenThrow(new RuntimeException("Mapping error"));
        when(mapper.toEntity(argThat((VehicleOfferDto d) -> d != null && goodOfferId.equals(d.getId()))))
                .thenReturn(new OfferEntity());


        lenient().when(mapper.toTinyLocatorEntity(any())).thenReturn(new TinyLocatorEntity());
        when(offerSqlRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<UUID> result = offerService.processBatch(List.of(goodDto, badDto), List.of());

        assertThat(result).containsExactly(goodOfferId);
    }
}