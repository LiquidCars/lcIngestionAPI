package net.liquidcars.ingestion.infra.mongodb;

import com.mongodb.bulk.BulkWriteResult;
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
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
    @DisplayName("processOffer: Debe actualizar el ID y guardar si la oferta entrante es más reciente")
    void processOffer_WhenOfferExistsAndIsNewer_ShouldUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity newEntity = new DraftOfferNoSQLEntity();
        DraftOfferNoSQLEntity existingEntity = new DraftOfferNoSQLEntity();
        UUID existingId = UUID.randomUUID();

        existingEntity.setCreatedAt(Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));
        newEntity.setCreatedAt(Instant.now());
        existingEntity.setId(existingId);

        when(mapper.toEntity(dto)).thenReturn(newEntity);

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(existingEntity);

        service.processOffer(dto);

        verify(repository, times(1)).save(newEntity);

        org.assertj.core.api.Assertions.assertThat(newEntity.getId()).isEqualTo(existingId);
    }

    @Test
    @DisplayName("processOffer: No debe actualizar si la oferta existente es más reciente")
    void processOffer_WhenOfferExistsButIsOlder_ShouldNotUpdate() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        DraftOfferNoSQLEntity newEntity = new DraftOfferNoSQLEntity();
        DraftOfferNoSQLEntity existingEntity = new DraftOfferNoSQLEntity();

        existingEntity.setCreatedAt(Instant.now().plus(1, ChronoUnit.DAYS));
        newEntity.setCreatedAt(Instant.now());

        when(mapper.toEntity(dto)).thenReturn(newEntity);

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(existingEntity);

        service.processOffer(dto);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("promoteDraftOffersToVehicleOffers: Debe procesar el flujo NoSQL correctamente")
    void promoteDraftOffers_NoSQLFlow_ShouldExecuteBulkOps() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = DraftOfferNoSQLEntityFactory.getDraftOfferNoSQLEntity();
        VehicleOfferNoSQLEntity vehicleEntity = new VehicleOfferNoSQLEntity();
        vehicleEntity.setId(draft.getId());
        OfferDto offerDto = OfferDto.builder().id(draft.getId()).build();

        List<DraftOfferNoSQLEntity> drafts = List.of(draft);
        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(inv -> drafts.stream());

        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(bulkOps);
        when(mongoTemplate.getConverter())
                .thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(bulkOps.execute())
                .thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        when(mapper.toVehicleOfferNoSQLEntity(draft)).thenReturn(vehicleEntity);
        when(mapper.toDto(draft)).thenReturn(offerDto);

        service.promoteDraftOffersToVehicleOffers(
                reportId,
                IngestionDumpType.INCREMENTAL,
                inventoryId,
                List.of(),
                List.of()
        );

        verify(bulkOps, atLeastOnce()).execute();

        verify(offerInfraSQLService).processBatch(
                argThat(list -> list.size() == 1 && list.get(0).getId().equals(draft.getId())),
                anyList()
        );
    }

    @Test
    @DisplayName("promoteDraftOffers: En modo REPLACEMENT debe ejecutar borrados en SQL y NoSQL")
    void promoteDraftOffers_ReplacementMode_ShouldDeleteObsolete() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        List<UUID> activeBookedOfferIds = List.of();

        DraftOfferNoSQLEntity fakeDraft = new DraftOfferNoSQLEntity();
        fakeDraft.setId(offerId);

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(invocation -> Stream.of(fakeDraft));

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        BulkOperations mockBulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(), eq(VehicleOfferNoSQLEntity.class))).thenReturn(mockBulkOps);

        com.mongodb.bulk.BulkWriteResult mockResult = mock(com.mongodb.bulk.BulkWriteResult.class);
        when(mockResult.getInsertedCount()).thenReturn(1);
        when(mockBulkOps.execute()).thenReturn(mockResult);

        OfferDto offerDto = new OfferDto();
        offerDto.setId(offerId);
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(new VehicleOfferNoSQLEntity());
        when(mapper.toDto(any())).thenReturn(offerDto);

        when(offerInfraSQLService.processBatch(anyList(), anyList())).thenReturn(List.of(offerId));

        service.promoteDraftOffersToVehicleOffers(reportId, IngestionDumpType.REPLACEMENT, inventoryId, null, activeBookedOfferIds);

        verify(vehicleOfferNoSqlRepository).deleteByInventoryIdAndIdNotIn(eq(inventoryId), anyList());

        verify(offerInfraSQLService).deleteOffersByInventoryIdExcludingIds(eq(inventoryId), anyList());
    }

    @Test
    @DisplayName("countOffersFromJobId: Debería relanzar error de DB como LCIngestionException")
    void countOffers_WhenDatabaseFails_ShouldThrowLCIngestionException() {
        UUID jobId = UUID.randomUUID();
        when(repository.countByJobIdentifier(jobId)).thenThrow(new RuntimeException("Mongo Down"));

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
        UUID invId = UUID.randomUUID();
        List<String> toDelete = List.of("REF1");
        List<UUID> activeBookedOfferIds = List.of();

        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.empty())
                .thenReturn(Stream.empty());

        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(mock(BulkOperations.class));

        com.mongodb.client.result.DeleteResult deleteResult = mock(com.mongodb.client.result.DeleteResult.class);
        when(deleteResult.getDeletedCount()).thenReturn(1L);

        when(mongoTemplate.remove(any(org.springframework.data.mongodb.core.query.Query.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(deleteResult);

        service.promoteDraftOffersToVehicleOffers(UUID.randomUUID(), IngestionDumpType.INCREMENTAL, invId, toDelete, activeBookedOfferIds);

        verify(offerInfraSQLService).deleteOffersByInventoryIdAndReferences(eq(invId), any());

        verify(mongoTemplate).remove(any(org.springframework.data.mongodb.core.query.Query.class), eq(VehicleOfferNoSQLEntity.class));
    }

    @Test
    @DisplayName("promoteDraftOffers: Cobertura de referencias nulas para entrar en los else")
    void promoteDraftOffers_NullReferences_Coverage() {
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());

        draft.setOwnerReference(null);
        draft.setDealerReference(null);
        draft.setChannelReference(null);

        VehicleOfferNoSQLEntity vehicleEntity = new VehicleOfferNoSQLEntity();
        vehicleEntity.setId(draft.getId());

        BulkOperations bulkOpsMock = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(bulkOpsMock);

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(bulkOpsMock.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.of(draft))
                .thenReturn(Stream.empty());

        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(vehicleEntity);

        service.promoteDraftOffersToVehicleOffers(
                reportId,
                IngestionDumpType.INCREMENTAL,
                UUID.randomUUID(),
                List.of(),
                List.of()
        );

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

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(
                        UUID.randomUUID(),
                        IngestionDumpType.INCREMENTAL,
                        UUID.randomUUID(),
                        List.of(),
                        List.of()
                )
        ).isInstanceOf(LCIngestionException.class);
    }

    @Test
    @DisplayName("promoteDraftOffersToVehicleOffers: Failure in SQL promotion should throw LCIngestionException")
    void promoteDraftOffersToVehicleOffers_SQL_Failure_Coverage() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();

        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());

        VehicleOfferNoSQLEntity productionEntity = new VehicleOfferNoSQLEntity();
        productionEntity.setId(draft.getId());
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(productionEntity);

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(inv -> Stream.of(draft));

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(bulkOps);
        when(bulkOps.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        OfferDto dto = new OfferDto();
        dto.setId(draft.getId());
        when(mapper.toDto(any())).thenReturn(dto);

        doThrow(new RuntimeException("SQL Connection Error"))
                .when(offerInfraSQLService).processBatch(anyList(), anyList());

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(
                        reportId,
                        IngestionDumpType.INCREMENTAL,
                        inventoryId,
                        List.of(),
                        List.of()
                )
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId);

        verify(offerInfraSQLService).processBatch(anyList(), anyList());
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
        draft.setId(UUID.randomUUID());

        VehicleOfferNoSQLEntity fakeProduction = new VehicleOfferNoSQLEntity();
        fakeProduction.setId(draft.getId());
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(fakeProduction);

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(inv -> Stream.of(draft));

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class))).thenReturn(bulkOps);
        when(bulkOps.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        OfferDto dto = new OfferDto();
        dto.setId(draft.getId());
        when(mapper.toDto(any())).thenReturn(dto);

        doThrow(new RuntimeException("Generic SQL Error"))
                .when(offerInfraSQLService).processBatch(anyList(), anyList());

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(
                        reportId,
                        IngestionDumpType.INCREMENTAL,
                        UUID.randomUUID(),
                        List.of(),
                        List.of()
                )
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId)
                .extracting("techCause")
                .isEqualTo(LCTechCauseEnum.DATABASE);

        verify(offerInfraSQLService).processBatch(anyList(), anyList());
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
    @DisplayName("processOffer: Debe lanzar LCIngestionException si falla la base de datos")
    void processOffer_Exception_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();
        when(mapper.toEntity(dto)).thenReturn(new DraftOfferNoSQLEntity());

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenThrow(new RuntimeException("DB Error"));

        assertThatThrownBy(() -> service.processOffer(dto))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("NoSQL persistence error for id:");

        verify(mongoTemplate).findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class));
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
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());
        draft.setOwnerReference("OWNER");
        draft.setDealerReference("");
        draft.setChannelReference(null);

        VehicleOfferNoSQLEntity vehicle = new VehicleOfferNoSQLEntity();
        vehicle.setId(draft.getId());

        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.of(draft))
                .thenReturn(Stream.empty());

        BulkOperations bulkOpsMock = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(bulkOpsMock);

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(bulkOpsMock.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(vehicle);

        service.promoteDraftOffersToVehicleOffers(
                UUID.randomUUID(),
                IngestionDumpType.INCREMENTAL,
                UUID.randomUUID(),
                List.of(),
                List.of()
        );

        verify(bulkOpsMock).upsert(any(), any());
        verify(bulkOpsMock).execute();
    }

    @Test
    @DisplayName("processBatchToSQL: Cobertura de fallo total en promoción SQL")
    void processBatchToSQL_TotalFailure_ShouldThrowLCIngestionException() {
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());
        VehicleOfferNoSQLEntity fakeProd = new VehicleOfferNoSQLEntity();
        fakeProd.setId(draft.getId());
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(fakeProd);

        when(mongoTemplate.stream(any(), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(inv -> Stream.of(draft));
        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(), eq(VehicleOfferNoSQLEntity.class))).thenReturn(bulkOps);
        when(bulkOps.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        when(mapper.toDto(any())).thenReturn(new OfferDto());

        doThrow(new RuntimeException("SQL Crash"))
                .when(offerInfraSQLService).processBatch(anyList(), anyList());

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(
                        reportId,
                        IngestionDumpType.INCREMENTAL,
                        UUID.randomUUID(),
                        List.of(),
                        List.of()
                )
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId)
                .extracting("techCause")
                .isEqualTo(LCTechCauseEnum.DATABASE);

        verify(offerInfraSQLService).processBatch(anyList(), anyList());
    }

    @Test
    @DisplayName("processOffer: Debe cubrir el catch de LCIngestionException")
    void processOffer_DatabaseException_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();

        when(mapper.toEntity(dto)).thenReturn(new DraftOfferNoSQLEntity());

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenThrow(new RuntimeException("Error Mongo"));

        assertThatThrownBy(() -> service.processOffer(dto))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("NoSQL persistence error for id: " + dto.getId())
                .extracting("techCause")
                .isEqualTo(LCTechCauseEnum.DATABASE);
    }

    @Test
    @DisplayName("promoteDraftOffersToSQL: Debe lanzar LCIngestionException cuando el batch SQL falla")
    void promoteDraftOffersToSQL_TotalFailure_Coverage() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());

        VehicleOfferNoSQLEntity fakeProduction = new VehicleOfferNoSQLEntity();
        fakeProduction.setId(draft.getId());
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(fakeProduction);

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(invocation -> Stream.of(draft));

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(), eq(VehicleOfferNoSQLEntity.class))).thenReturn(bulkOps);
        when(bulkOps.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        OfferDto dto = new OfferDto();
        dto.setId(draft.getId());
        when(mapper.toDto(any())).thenReturn(dto);

        doThrow(new RuntimeException("SQL Critical Failure"))
                .when(offerInfraSQLService).processBatch(anyList(), anyList());

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(
                        reportId,
                        IngestionDumpType.INCREMENTAL,
                        inventoryId,
                        List.of(),
                        List.of()
                )
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed: " + reportId);

        verify(offerInfraSQLService).processBatch(anyList(), anyList());
    }

    @Test
    @DisplayName("processOffer: Debe actualizar si el existente tiene fecha null")
    void processOffer_UpdateNullDate_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();

        DraftOfferNoSQLEntity incoming = new DraftOfferNoSQLEntity();
        incoming.setCreatedAt(Instant.now());

        DraftOfferNoSQLEntity existing = new DraftOfferNoSQLEntity();
        existing.setId(UUID.randomUUID());
        existing.setCreatedAt(null);

        when(mapper.toEntity(dto)).thenReturn(incoming);

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(existing);

        service.processOffer(dto);

        assertThat(incoming.getId()).isEqualTo(existing.getId());
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
        existing.setId(UUID.randomUUID());
        existing.setCreatedAt(null);

        when(mapper.toEntity(dto)).thenReturn(incoming);

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(existing);

        service.processOffer(dto);

        verify(repository).save(incoming);
        assertThat(incoming.getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("processBatchToSQL: Debe propagar LCIngestionException y capturar el fallo global")
    void processBatchToSQL_PropagateDomainException_Coverage() {
        UUID reportId = UUID.randomUUID();
        DraftOfferNoSQLEntity draft = new DraftOfferNoSQLEntity();
        draft.setId(UUID.randomUUID());

        VehicleOfferNoSQLEntity fakeProduction = new VehicleOfferNoSQLEntity();
        fakeProduction.setId(draft.getId());
        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(fakeProduction);

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenAnswer(inv -> Stream.of(draft));

        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        BulkOperations bulkOps = mock(BulkOperations.class);
        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class))).thenReturn(bulkOps);
        when(bulkOps.execute()).thenReturn(mock(com.mongodb.bulk.BulkWriteResult.class));

        OfferDto dto = new OfferDto();
        dto.setId(draft.getId());
        when(mapper.toDto(any())).thenReturn(dto);

        LCIngestionException domainEx = LCIngestionException.builder()
                .message("Batch domain error")
                .techCause(LCTechCauseEnum.DATABASE)
                .build();

        doThrow(domainEx).when(offerInfraSQLService).processBatch(anyList(), anyList());

        assertThatThrownBy(() ->
                service.promoteDraftOffersToVehicleOffers(
                        reportId,
                        IngestionDumpType.INCREMENTAL,
                        UUID.randomUUID(),
                        List.of(),
                        List.of()
                )
        )
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error in promotion all offers are not processed");

        verify(offerInfraSQLService).processBatch(anyList(), anyList());
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
        int daysOld = 30;
        long expectedDeletedCount = 50L;

        when(repository.deleteByBatchStatusNotCompletedAndUpdatedAtBefore(any(Instant.class)))
                .thenReturn(expectedDeletedCount);

        service.purgeObsoleteOffers(daysOld);

        verify(repository, times(1)).deleteByBatchStatusNotCompletedAndUpdatedAtBefore(any(Instant.class));
    }

    @Test
    void processOffer_UpdateWithExistingDate_Coverage() {
        OfferDto dto = OfferDtoFactory.getOfferDto();

        DraftOfferNoSQLEntity incoming = new DraftOfferNoSQLEntity();
        incoming.setCreatedAt(Instant.now().plusSeconds(100));

        DraftOfferNoSQLEntity existing = new DraftOfferNoSQLEntity();
        existing.setId(UUID.randomUUID());
        existing.setCreatedAt(Instant.now());

        when(mapper.toEntity(dto)).thenReturn(incoming);

        when(mongoTemplate.findOne(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(existing);

        service.processOffer(dto);

        verify(repository).save(incoming);
        assertThat(incoming.getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("Promote: Debe ignorar campos nulos al construir el Update")
    void promote_ShouldSkipNullValuesInDocument() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();
        DraftOfferNoSQLEntity draftWithNulls = new DraftOfferNoSQLEntity();
        draftWithNulls.setId(UUID.randomUUID());

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(Stream.of(draftWithNulls)).thenReturn(Stream.empty());

        VehicleOfferNoSQLEntity productionEntity = new VehicleOfferNoSQLEntity();
        productionEntity.setId(draftWithNulls.getId());
        when(mapper.toVehicleOfferNoSQLEntity(draftWithNulls)).thenReturn(productionEntity);

        Document docWithNull = new Document("field1", "value1").append("field2", null);
        MongoConverter mockConverter = mock(MongoConverter.class);
        when(mongoTemplate.getConverter()).thenReturn(mockConverter);

        doAnswer(inv -> {
            Document d = inv.getArgument(1);
            d.putAll(docWithNull);
            return null;
        }).when(mockConverter).write(eq(productionEntity), any(Document.class));

        BulkOperations mockBulkOps = mock(BulkOperations.class);
        BulkWriteResult mockResult = mock(BulkWriteResult.class);

        when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class), eq(VehicleOfferNoSQLEntity.class)))
                .thenReturn(mockBulkOps);

        when(mockBulkOps.execute()).thenReturn(mockResult);
        when(mockResult.getInsertedCount()).thenReturn(1);
        when(mockResult.getModifiedCount()).thenReturn(0);
        when(mockResult.getUpserts()).thenReturn(Collections.emptyList());

        service.promoteDraftOffersToVehicleOffers(
                reportId,
                IngestionDumpType.INCREMENTAL,
                inventoryId,
                List.of(),
                List.of()
        );

        verify(mongoTemplate, atLeastOnce()).getConverter();
        verify(mockBulkOps).execute();
    }

    @Test
    @DisplayName("Promote: Debe ejecutar bulkOps cuando se alcanza el tamaño de batch (100)")
    void promote_ShouldExecuteBulkWhenBatchSizeReached() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();

        List<DraftOfferNoSQLEntity> drafts = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            DraftOfferNoSQLEntity d = new DraftOfferNoSQLEntity();
            d.setId(UUID.randomUUID());
            drafts.add(d);
        }

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenReturn(drafts.stream())
                .thenReturn(Stream.empty());

        when(mapper.toVehicleOfferNoSQLEntity(any())).thenReturn(new VehicleOfferNoSQLEntity());
        when(mongoTemplate.getConverter()).thenReturn(mock(MongoConverter.class));

        BulkOperations mockBulk = mock(BulkOperations.class);
        BulkWriteResult mockResult = mock(BulkWriteResult.class);
        when(mockResult.getInsertedCount()).thenReturn(1);

        when(mongoTemplate.bulkOps(any(), eq(VehicleOfferNoSQLEntity.class))).thenReturn(mockBulk);
        when(mockBulk.execute()).thenReturn(mockResult);

        service.promoteDraftOffersToVehicleOffers(
                reportId,
                IngestionDumpType.INCREMENTAL,
                inventoryId,
                List.of(),
                List.of()
        );

        verify(mockBulk, atLeast(2)).execute();
    }

    @Test
    @DisplayName("countOffersFromJobId: Debe retornar el conteo correctamente")
    void countOffersFromJobId_Success_Coverage() {
        UUID jobId = UUID.randomUUID();
        long expectedCount = 5L;

        when(repository.countByJobIdentifier(jobId)).thenReturn(expectedCount);

        long result = service.countOffersFromJobId(jobId);

        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(expectedCount);

        verify(repository, times(1)).countByJobIdentifier(jobId);
    }

    @Test
    @DisplayName("replaceOffersInSQL: Cobertura de rama cuando la lista de IDs está vacía")
    void replaceOffersInSQL_EmptyList_Coverage() {
        UUID inventoryId = UUID.fromString("12ce40d7-d49b-41ef-8ca4-9644f1eb4249");
        List<UUID> emptyPromotedIds = Collections.emptyList();
        long expectedDeletedCount = 10L;

        when(offerInfraSQLService.deleteOffersByInventoryId(inventoryId))
                .thenReturn(expectedDeletedCount);

        long result = (long) org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                service,
                "replaceOffersInSQL",
                inventoryId,
                emptyPromotedIds
        );

        verify(offerInfraSQLService, times(1)).deleteOffersByInventoryId(inventoryId);
        org.assertj.core.api.Assertions.assertThat(result).isEqualTo(expectedDeletedCount);
    }

    @Test
    @DisplayName("promoteDraftOffersToSQL: Cobertura de error crítico en la promoción SQL")
    void promoteDraftOffersToSQL_CriticalError_Coverage() {
        UUID reportId = UUID.randomUUID();
        List<UUID> activeBookedOfferIds = java.util.Collections.emptyList();

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenThrow(new RuntimeException("MongoDB Connection Failure"));

        assertThatThrownBy(() ->
                org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                        service,
                        "promoteDraftOffersToSQL",
                        reportId,
                        activeBookedOfferIds))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error during SQL promotion for ingestionReportId: " + reportId);
    }

    @Test
    @DisplayName("promoteDraftAndGetsPromoted: Cobertura de error crítico en el bloque catch externo")
    void promoteDraftAndGetsPromoted_ExternalCatch_Coverage() {
        UUID reportId = UUID.randomUUID();
        UUID inventoryId = UUID.randomUUID();

        List<UUID> activeBookedOfferIds = Collections.emptyList();

        when(mongoTemplate.stream(any(Query.class), eq(DraftOfferNoSQLEntity.class)))
                .thenThrow(new RuntimeException("Critical Mongo Failure"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                                service,
                                "promoteDraftOffersAndGetsPromoted",
                                reportId,
                                inventoryId,
                                activeBookedOfferIds))
                .isInstanceOf(LCIngestionException.class)
                .hasMessageContaining("Error during NoSQL promotion for report: " + reportId)
                .extracting("techCause")
                .isEqualTo(LCTechCauseEnum.DATABASE);
    }
}
