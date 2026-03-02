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
    @DisplayName("deleteByReferences should return 0 if references list is empty")
    void deleteByReferences_EmptyList_ReturnsZero() {
        long result = offerService.deleteOffersByInventoryIdAndReferences(inventoryId, Collections.emptyList());

        assertThat(result).isZero();
        verifyNoInteractions(offerSqlRepository);
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

}
