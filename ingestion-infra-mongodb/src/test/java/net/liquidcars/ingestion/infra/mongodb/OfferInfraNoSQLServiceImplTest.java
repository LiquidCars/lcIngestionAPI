package net.liquidcars.ingestion.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.factory.DraftOfferNoSQLEntityFactory;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.infra.mongodb.entity.DraftOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.entity.VehicleOfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.DraftOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.repository.VehicleOfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.OfferInfraNoSQLServiceImpl;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class OfferInfraNoSQLServiceImplTest {

    @Mock
    private DraftOfferNoSqlRepository repository;

    @Mock
    private OfferInfraNoSQLMapper mapper;

    @Mock private MongoTemplate mongoTemplate;

    @Mock private IOfferInfraSQLService offerInfraSQLService;

    @Mock private VehicleOfferNoSqlRepository vehicleOfferNoSqlRepository;

    @InjectMocks
    private OfferInfraNoSQLServiceImpl service;

    @Test
    void save_ShouldMapAndPersistInMongo() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity entity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();

        when(mapper.toEntity(dto)).thenReturn(entity);

        service.processOffer(dto);

        verify(mapper, times(1)).toEntity(dto);
        verify(repository, times(1)).save(entity);
    }

    @Test
    void processOffer_WhenOfferExistsAndIsNewer_ShouldUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity newEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        DraftOfferNoSQLEntity existingEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        UUID existingId = UUID.randomUUID();

        existingEntity.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        newEntity.setCreatedAt(Instant.now());
        existingEntity.setId(existingId);

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(repository.findById(dto.getId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(repository, times(1)).save(newEntity);
        assert(newEntity.getId().equals(existingId));
    }

    @Test
    void processOffer_WhenOfferExistsButIsOlder_ShouldNotUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity newEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        DraftOfferNoSQLEntity existingEntity = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();

        // existing is artificially in the future → guarantees it is newer
        existingEntity.setCreatedAt(Instant.now().plus(1, ChronoUnit.DAYS));

        when(mapper.toEntity(dto)).thenReturn(newEntity);
        when(repository.findById(dto.getId()))
                .thenReturn(Optional.of(existingEntity));

        service.processOffer(dto);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("promoteDraftOffersToVehicleOffers: Debe procesar el flujo NoSQL correctamente")
    void promoteDraftOffers_NoSQLFlow_ShouldExecuteBulkOps() {
        // GIVEN
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        VehicleOfferNoSQLEntity vehicleEntity = new VehicleOfferNoSQLEntity();
        vehicleEntity.setId(draft.getId());
        OfferDto offerDto = OfferDto.builder().id(draft.getId()).build();

        // IMPORTANTE: Usamos .thenReturn(...) múltiple para que cada llamada reciba un Stream nuevo
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.of(draft))  // Primera llamada (NoSQL)
                .thenReturn(Stream.of(draft)); // Segunda llamada (SQL)

        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class))).thenReturn(bulkOps);
        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(bulkOps.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        // Configuramos los mappers para evitar el NullPointerException
        when(mapper.toVehicleOfferNoSQLEntity(draft)).thenReturn(vehicleEntity);
        when(mapper.toDto(draft)).thenReturn(offerDto);

        // WHEN
        service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, inventoryId, null);

        // THEN
        verify(bulkOps, atLeastOnce()).execute();
        verify(offerInfraSQLService, atLeastOnce()).processOffer(any());
    }

    @Test
    @DisplayName("promoteDraftOffers: En modo REPLACEMENT debe ejecutar borrados en SQL y NoSQL")
    void promoteDraftOffers_ReplacementMode_ShouldDeleteObsolete() {
        // GIVEN
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();

        // Importante: Devolvemos streams vacíos para simular que no hay nada que promocionar
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.empty())
                .thenReturn(Stream.empty());

        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(mock(BulkOperations.class));

        // WHEN
        service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.REPLACEMENT, inventoryId, null);

        // THEN
        // Verificamos que en NoSQL se intenta borrar todo lo que NO esté en la lista vacía
        verify(vehicleOfferNoSqlRepository).deleteByInventoryIdAndIdNotIn(eq(inventoryId), anyList());

        // CORRECCIÓN: Como la lista está vacía, el código llama a deleteOffersByInventoryId, no al ExcludingIds
        verify(offerInfraSQLService).deleteOffersByInventoryId(inventoryId);
    }

    @Test
    @DisplayName("countOffersFromJobId: Debería relanzar error de DB como LCIngestionException")
    void countOffers_WhenDatabaseFails_ShouldThrowLCIngestionException() {
        // GIVEN
        UUID jobId = UUID.randomUUID();
        when(repository.countByJobIdentifier(jobId)).thenThrow(new RuntimeException("Mongo Down"));

        // WHEN & THEN
        assertThatThrownBy(() -> service.countOffersFromJobId(jobId))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Failed to get offers from NoSQL by jobId");
    }

    @Test
    @DisplayName("purgeObsoleteOffers: Debe lanzar LCIngestionException si falla la DB")
    void purgeObsoleteOffers_Exception() {
        when(repository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(any())).thenThrow(new RuntimeException());
        assertThatThrownBy(() -> service.purgeObsoleteOffers(30)).isInstanceOf(LCIngestionException.class);
    }

    @Test
    @DisplayName("deleteOffersInPromotion: Debe cubrir NoSQL y SQL con referencias explícitas")
    void deleteOffersInPromotion_FullCoverage() {
        // GIVEN
        UUID invId = UUID.randomUUID();
        List<String> toDelete = List.of("REF1");

        // Configuración necesaria para que el flujo principal no explote antes de llegar al borrado
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.empty())  // Para NoSQL
                .thenReturn(Stream.empty()); // Para SQL

        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(mock(BulkOperations.class));

        // Mock para NoSQL (mongoTemplate.remove devuelve DeleteResult)
        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(1L);

        when(mongoTemplate.remove(any(org.springframework.data.mongodb.core.query.Query.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(deleteResult);

        // WHEN
        service.promoteDraftOffersToVehicleOffers(UUID.randomUUID(), IngestionDumpType.UPDATE, invId, toDelete);

        // THEN
        verify(offerInfraSQLService).deleteOffersByInventoryIdAndReferences(eq(invId), eq(toDelete));
        verify(mongoTemplate).remove(any(org.springframework.data.mongodb.core.query.Query.class), eq(VehicleOfferNoSQLEntity.class));
    }

    @Test
    @DisplayName("promoteDraftOffers: Cobertura de referencias nulas para entrar en los else")
    void promoteDraftOffers_NullReferences_Coverage() {
        // GIVEN
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());
        // Referencias nulas explícitamente
        draft.setOwnerReference(null);
        draft.setDealerReference(null);
        draft.setChannelReference(null);

        VehicleOfferNoSQLEntity vehicleEntity = new VehicleOfferNoSQLEntity();
        vehicleEntity.setId(draft.getId());

        // Resolvemos la ambigüedad de bulkOps con el cast y any(Class.class)
        BulkOperations bulkOpsMock = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(bulkOpsMock);

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(bulkOpsMock.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        // Simulamos streams para NoSQL y SQL
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.of(draft))
                .thenReturn(Stream.empty());

        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(vehicleEntity);

        // WHEN
        service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, UUID.randomUUID(), null);

        // THEN
        verify(bulkOpsMock).upsert(any(), any());
        verify(mapper).toVehicleOfferNoSQLEntity(draft);
    }

    @Test
    @DisplayName("promoteDraftOffers: Error total en promoción debe lanzar excepción")
    void promoteDraftOffers_TotalFailure_Exception() {
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class))).thenReturn(Stream.of(draft));
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenThrow(new RuntimeException("Mapping error"));

        assertThatThrownBy(() -> service.promoteDraftOffersToVehicleOffers(UUID.randomUUID(), IngestionDumpType.UPDATE, UUID.randomUUID(), null))
                .isInstanceOf(LCIngestionException.class);
    }

    @Test
    @DisplayName("promoteDraftOffersToVehicleOffers: Failure in SQL promotion should throw LCIngestionException")
    void promoteDraftOffersToVehicleOffers_SQL_Failure_Coverage() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());

        OfferDto dto = new OfferDto();
        dto.setId(draft.getId());

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.empty())
                .thenReturn(Stream.of(draft));

        when(mapper.toDto(draft)).thenReturn(dto);

        doThrow(new RuntimeException("SQL Connection Error"))
                .when(offerInfraSQLService).processOffer(any(OfferDto.class));

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, inventoryId, null)
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId)
                .extracting("techCause")
                .isEqualTo(LCTechCauseEnum.DATABASE);

        verify(offerInfraSQLService).processOffer(dto);
    }

    @Test
    @DisplayName("countOffersFromReportId: Cobertura positiva")
    void countOffersFromReportId_ShouldReturnCount() {
        UUID reportId = UUID.randomUUID();
        when(repository.countByIngestionReportId(reportId)).thenReturn(10L);

        long result = service.countOffersFromReportId(reportId);

        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("deleteDraftOffersByIngestionReportId: Cobertura positiva")
    void deleteDraftOffersByIngestionReportId_ShouldInvokeRepo() {
        UUID reportId = UUID.randomUUID();
        service.deleteDraftOffersByIngestionReportId(reportId);
        verify(repository).deleteByIngestionReportId(reportId);
    }

    @Test
    @DisplayName("processBatchToSQL: Cobertura de excepción técnica genérica")
    void processBatchToSQL_TechnicalException_ShouldThrowLCIngestionException() {
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        OfferDto dto = new OfferDto();
        dto.setId(UUID.randomUUID());

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.empty())
                .thenReturn(Stream.of(draft));

        when(mapper.toDto(draft)).thenReturn(dto);

        doThrow(new RuntimeException("Generic SQL Error"))
                .when(offerInfraSQLService).processOffer(any(OfferDto.class));

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, UUID.randomUUID(), null)
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId);

        verify(offerInfraSQLService).processOffer(dto);
    }

    @Test
    @DisplayName("countOffersFromReportId: Excepción debe lanzar LCIngestionException")
    void countOffersFromReportId_Exception() {
        UUID reportId = UUID.randomUUID();
        when(repository.countByIngestionReportId(reportId)).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> service.countOffersFromReportId(reportId))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Failed to get offers from NoSQL by ingestionReportId");
    }

    @Test
    @DisplayName("deleteDraftOffersByIngestionReportId: Cobertura de log")
    void deleteDraftOffersByIngestionReportId_Success() {
        UUID reportId = UUID.randomUUID();
        when(repository.deleteByIngestionReportId(reportId)).thenReturn(5L);

        service.deleteDraftOffersByIngestionReportId(reportId);

        verify(repository).deleteByIngestionReportId(reportId);
    }

    @Test
    @DisplayName("processOffer: Debe lanzar LCIngestionException si falla el repositorio")
    void processOffer_Exception_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        when(mapper.toEntity(dto)).thenReturn(new DraftOfferNoSQLEntity());
        when(repository.findById(any())).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> service.processOffer(dto))
                .isInstanceOf(LCIngestionException.class);
    }

    @Test
    @DisplayName("countOffersFromReportId: Excepción en repo")
    void countOffersFromReportId_Exception_Coverage() {
        UUID id = UUID.randomUUID();
        when(repository.countByIngestionReportId(id)).thenThrow(new RuntimeException());
        assertThatThrownBy(() -> service.countOffersFromReportId(id)).isInstanceOf(LCIngestionException.class);
    }

    @Test
    @DisplayName("promoteDraftOffers: Cobertura de referencias parciales (mix de null y empty)")
    void promoteDraftOffers_PartialReferences_Coverage() {
        // GIVEN
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());
        draft.setOwnerReference("OWNER"); // Entra en el if
        draft.setDealerReference("");    // Entra en el else (empty)
        draft.setChannelReference(null); // Entra en el else (null)

        VehicleOfferNoSQLEntity vehicle = new VehicleOfferNoSQLEntity();
        vehicle.setId(draft.getId());

        // Mock del Stream (doble retorno para NoSQL y SQL)
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.of(draft))
                .thenReturn(Stream.empty());

        // SOLUCIÓN AMBIGÜEDAD: Usar eq() con la clase específica
        BulkOperations bulkOpsMock = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(bulkOpsMock);

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(bulkOpsMock.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(vehicle);

        // WHEN
        service.promoteDraftOffersToVehicleOffers(UUID.randomUUID(), IngestionDumpType.UPDATE, UUID.randomUUID(), null);

        // THEN
        verify(bulkOpsMock).upsert(any(), any());
        verify(bulkOpsMock).execute();
    }

    @Test
    @DisplayName("processBatchToSQL: Cobertura de LCIngestionException (Rama if)")
    void processBatchToSQL_LCIngestionException_Coverage() {
        // Arrange
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();

        when(mongoTemplate.stream(any(), any()))
                .thenReturn(Stream.empty())    // NoSQL
                .thenReturn(Stream.of(draft)); // SQL

        when(mapper.toDto(any())).thenReturn(new OfferDto());

        // Lanzamos la excepción de dominio específicamente
        LCIngestionException domainError = LCIngestionException.builder()
                .message("Domain Error")
                .build();

        doThrow(domainError)
                .when(offerInfraSQLService).processOffer(any());

        // Act & Assert
        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, UUID.randomUUID(), null)
        )
                .isInstanceOf(LCIngestionException.class)
                // El batch atrapa el error individual, pero al ser el único,
                // acaba lanzando el mensaje de "Error in promotion..."
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId);

        verify(offerInfraSQLService).processOffer(any());
    }

    @Test
    @DisplayName("processOffer: Debe cubrir el catch de LCIngestionException")
    void processOffer_DatabaseException_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        when(mapper.toEntity(dto)).thenReturn(new DraftOfferNoSQLEntity());
        // Forzamos error en el findById para entrar al catch
        when(repository.findById(any())).thenThrow(new RuntimeException("Error Mongo"));

        assertThatThrownBy(() -> service.processOffer(dto))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("NoSQL persistence error");
    }

    @Test
    @DisplayName("promoteDraftOffersToSQL: Debe lanzar LCIngestionException genérica cuando el proceso SQL falla")
    void promoteDraftOffersToSQL_TotalFailure_Coverage() {
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        OfferDto dto = new OfferDto();
        dto.setId(UUID.randomUUID());

        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.empty())
                .thenReturn(Stream.of(draft));

        when(mapper.toDto(any())).thenReturn(dto);

        doThrow(new RuntimeException("SQL Critical Failure"))
                .when(offerInfraSQLService).processOffer(any());

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, UUID.randomUUID(), null)
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId);

        verify(offerInfraSQLService, atLeastOnce()).processOffer(any());
    }

    @Test
    @DisplayName("processOffer: Debe actualizar si la fecha es igual o el existente es null")
    void processOffer_UpdateNullDate_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity incoming = new DraftOfferNoSQLEntity();
        incoming.setCreatedAt(Instant.now());

        DraftOfferNoSQLEntity existing = new DraftOfferNoSQLEntity();
        existing.setCreatedAt(null); // Esto forzará shouldUpdate = true

        when(mapper.toEntity(dto)).thenReturn(incoming);
        when(repository.findById(any())).thenReturn(Optional.of(existing));

        service.processOffer(dto);

        verify(repository).save(incoming);
    }

    @Test
    @DisplayName("purgeObsoleteOffers: Debe lanzar LCIngestionException en caso de error")
    void purgeObsoleteOffers_TechnicalError_Coverage() {
        when(repository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(any()))
                .thenThrow(new RuntimeException("Mongo connection error"));

        assertThatThrownBy(() -> service.purgeObsoleteOffers(30))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error during NoSQL offers data purge");
    }

    @Test
    @DisplayName("processOffer: Debe actualizar si la fecha existente es null")
    void processOffer_UpdateExistingNullDate_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity incoming = new DraftOfferNoSQLEntity();
        incoming.setCreatedAt(Instant.now());

        DraftOfferNoSQLEntity existing = new DraftOfferNoSQLEntity();
        existing.setCreatedAt(null); // Esto fuerza shouldUpdate = true

        when(mapper.toEntity(dto)).thenReturn(incoming);
        when(repository.findById(any())).thenReturn(Optional.of(existing));

        service.processOffer(dto);

        verify(repository).save(incoming);
    }

    @Test
    @DisplayName("processBatchToSQL: Debe propagar LCIngestionException y capturar el fallo global")
    void processBatchToSQL_PropagateDomainException_Coverage() {
        // GIVEN
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        OfferDto dto = new OfferDto();
        dto.setId(UUID.randomUUID());

        when(mongoTemplate.stream(any(), any()))
                .thenReturn(Stream.empty())    // NoSQL parte
                .thenReturn(Stream.of(draft)); // SQL parte

        when(mapper.toDto(any())).thenReturn(dto);

        // Lanzamos la excepción de dominio
        LCIngestionException domainEx = LCIngestionException.builder()
                .message("Batch domain error")
                .build();

        doThrow(domainEx).when(offerInfraSQLService).processOffer(any());

        // WHEN & THEN
        // Capturamos la excepción que lanza promoteDraftOffersToSQL cuando totalProcessed == 0
        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.UPDATE, UUID.randomUUID(), null)
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed");

        // Verificamos que se intentó la llamada que disparó el error
        verify(offerInfraSQLService).processOffer(any());
    }

    @Test
    @DisplayName("countOffersFromJobId: Error técnico debe lanzar LCIngestionException")
    void countOffersFromJobId_Exception_Coverage() {
        UUID jobId = UUID.randomUUID();
        when(repository.countByJobIdentifier(jobId)).thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> service.countOffersFromJobId(jobId))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Failed to get offers from NoSQL by jobId");
    }

    @Test
    @DisplayName("purgeObsoleteOffers: Debe ejecutar el borrado y loguear el número de ofertas eliminadas")
    void purgeObsoleteOffers_Success_Coverage() {
        // GIVEN
        int daysOld = 30;
        long expectedDeletedCount = 50L;

        // Simulamos que el repositorio devuelve 50 registros borrados
        when(repository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(any(Instant.class)))
                .thenReturn(expectedDeletedCount);

        // WHEN
        service.purgeObsoleteOffers(daysOld);

        // THEN
        // Verificamos que se llamó al repositorio con una instancia de tiempo
        verify(repository, times(1)).deleteByBatchStatusNotCompletedAndUpdatedAtBefore(any(Instant.class));
    }

    @Test
    void processOffer_UpdateWithExistingDate_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity incoming = new DraftOfferNoSQLEntity();
        incoming.setCreatedAt(Instant.now().plusSeconds(100));
        DraftOfferNoSQLEntity existing = new DraftOfferNoSQLEntity();
        existing.setCreatedAt(Instant.now()); // Fecha presente para cubrir el 'if'

        when(mapper.toEntity(dto)).thenReturn(incoming);
        when(repository.findById(any())).thenReturn(Optional.of(existing));
        service.processOffer(dto);
        verify(repository).save(incoming);
    }

}
