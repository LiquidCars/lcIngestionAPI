package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.VehicleInstanceDto;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.batch.*;
import org.instancio.Instancio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.instancio.Select.field;

/**
 * Factory class for creating test data objects using Instancio.
 * Provides consistent test data creation across all test classes.
 */
public class TestDataFactory {

    // ==================== OfferDto Factories ====================

    public static OfferDto createOfferDto() {
        return Instancio.of(OfferDto.class)
                .set(field(OfferDto::getId), UUID.randomUUID())
                .set(field(OfferDto::getIngestionReportId), (UUID) null)
                .set(field(OfferDto::getJobIdentifier), (UUID) null)
                .set(field(OfferDto::getLastUpdated), System.currentTimeMillis())
                .ignore(field(OfferDto::getHash))
                .create();
    }

    public static OfferDto createOfferDtoWithExternalId(String externalId) {
        ExternalIdInfoDto externalIdInfo = Instancio.of(ExternalIdInfoDto.class)
                .set(field(ExternalIdInfoDto::getOwnerReference), externalId)
                .create();

        return Instancio.of(OfferDto.class)
                .set(field(OfferDto::getId), UUID.randomUUID())
                .set(field(OfferDto::getExternalIdInfo), externalIdInfo)
                .set(field(OfferDto::getIngestionReportId), (UUID) null)
                .set(field(OfferDto::getJobIdentifier), (UUID) null)
                .ignore(field(OfferDto::getHash))
                .create();
    }

    public static OfferDto createOfferDtoWithReportId(UUID reportId) {
        return Instancio.of(OfferDto.class)
                .set(field(OfferDto::getId), UUID.randomUUID())
                .set(field(OfferDto::getIngestionReportId), reportId)
                .set(field(OfferDto::getJobIdentifier), (UUID) null)
                .ignore(field(OfferDto::getHash))
                .create();
    }

    public static List<OfferDto> createOfferDtoList(int count) {
        return Instancio.ofList(OfferDto.class)
                .size(count)
                .set(field(OfferDto::getIngestionReportId), (UUID) null)
                .set(field(OfferDto::getJobIdentifier), (UUID) null)
                .ignore(field(OfferDto::getHash))
                .create();
    }

    // ==================== IngestionBatchReportDto Factories ====================

    public static IngestionBatchReportDto createBatchReport() {
        return Instancio.of(IngestionBatchReportDto.class)
                .set(field(IngestionBatchReportDto::getJobId), UUID.randomUUID())
                .set(field(IngestionBatchReportDto::getStatus), IngestionBatchStatus.STARTED)
                .set(field(IngestionBatchReportDto::getReadCount), 0L)
                .set(field(IngestionBatchReportDto::getWriteCount), 0L)
                .set(field(IngestionBatchReportDto::getSkipCount), 0L)
                .set(field(IngestionBatchReportDto::isProcessed), false)
                .set(field(IngestionBatchReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionBatchReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    public static IngestionBatchReportDto createBatchReportWithStatus(
            UUID jobId,
            IngestionBatchStatus status) {
        return Instancio.of(IngestionBatchReportDto.class)
                .set(field(IngestionBatchReportDto::getJobId), jobId)
                .set(field(IngestionBatchReportDto::getStatus), status)
                .set(field(IngestionBatchReportDto::isProcessed), false)
                .set(field(IngestionBatchReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionBatchReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    public static IngestionBatchReportDto createBatchReportWithCounts(
            UUID jobId,
            IngestionBatchStatus status,
            long readCount,
            long writeCount,
            long skipCount) {
        return Instancio.of(IngestionBatchReportDto.class)
                .set(field(IngestionBatchReportDto::getJobId), jobId)
                .set(field(IngestionBatchReportDto::getStatus), status)
                .set(field(IngestionBatchReportDto::getReadCount), readCount)
                .set(field(IngestionBatchReportDto::getWriteCount), writeCount)
                .set(field(IngestionBatchReportDto::getSkipCount), skipCount)
                .set(field(IngestionBatchReportDto::isProcessed), false)
                .set(field(IngestionBatchReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionBatchReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    public static IngestionBatchReportDto createCompletedBatchReport(UUID jobId, long count) {
        return createBatchReportWithCounts(
                jobId,
                IngestionBatchStatus.COMPLETED,
                count,
                count,
                0L
        );
    }

    public static IngestionBatchReportDto createFailedBatchReport(UUID jobId) {
        return createBatchReportWithStatus(jobId, IngestionBatchStatus.FAILED);
    }

    // ==================== IngestionReportDto Factories ====================

    public static IngestionReportDto createIngestionReport() {
        return Instancio.of(IngestionReportDto.class)
                .set(field(IngestionReportDto::getId), UUID.randomUUID())
                .set(field(IngestionReportDto::getStatus), IngestionBatchStatus.STARTED)
                .set(field(IngestionReportDto::isProcessed), false)
                .set(field(IngestionReportDto::getProcessType), IngestionProcessType.PROCESS)
                .set(field(IngestionReportDto::getDumpType), IngestionDumpType.REPLACEMENT)
                .set(field(IngestionReportDto::getReadCount), 0)
                .set(field(IngestionReportDto::getWriteCount), 0)
                .set(field(IngestionReportDto::getSkipCount), 0)
                .set(field(IngestionReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    public static IngestionReportDto createIngestionReportWithBatchJobId(UUID batchJobId) {
        return Instancio.of(IngestionReportDto.class)
                .set(field(IngestionReportDto::getId), UUID.randomUUID())
                .set(field(IngestionReportDto::getBatchJobId), batchJobId)
                .set(field(IngestionReportDto::getStatus), IngestionBatchStatus.STARTED)
                .set(field(IngestionReportDto::isProcessed), false)
                .set(field(IngestionReportDto::getProcessType), IngestionProcessType.FILE)
                .set(field(IngestionReportDto::getDumpType), IngestionDumpType.REPLACEMENT)
                .set(field(IngestionReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    public static IngestionReportDto createIngestionReportWithDetails(
            UUID reportId,
            UUID participantId,
            UUID inventoryId,
            int writeCount) {
        return Instancio.of(IngestionReportDto.class)
                .set(field(IngestionReportDto::getId), reportId)
                .set(field(IngestionReportDto::getRequesterParticipantId), participantId)
                .set(field(IngestionReportDto::getInventoryId), inventoryId)
                .set(field(IngestionReportDto::getWriteCount), writeCount)
                .set(field(IngestionReportDto::getReadCount), writeCount)
                .set(field(IngestionReportDto::getSkipCount), 0)
                .set(field(IngestionReportDto::isProcessed), false)
                .set(field(IngestionReportDto::getStatus), IngestionBatchStatus.STARTED)
                .set(field(IngestionReportDto::getProcessType), IngestionProcessType.PROCESS)
                .set(field(IngestionReportDto::getDumpType), IngestionDumpType.REPLACEMENT)
                .set(field(IngestionReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    public static IngestionReportDto createCompletedIngestionReport(UUID reportId, long count) {
        return Instancio.of(IngestionReportDto.class)
                .set(field(IngestionReportDto::getId), reportId)
                .set(field(IngestionReportDto::getStatus), IngestionBatchStatus.COMPLETED)
                .set(field(IngestionReportDto::getWriteCount), count)
                .set(field(IngestionReportDto::getReadCount), count)
                .set(field(IngestionReportDto::getSkipCount), 0)
                .set(field(IngestionReportDto::isProcessed), true)
                .set(field(IngestionReportDto::getProcessType), IngestionProcessType.PROCESS)
                .set(field(IngestionReportDto::getDumpType), IngestionDumpType.REPLACEMENT)
                .set(field(IngestionReportDto::getCreatedAt), OffsetDateTime.now())
                .set(field(IngestionReportDto::getUpdatedAt), OffsetDateTime.now())
                .create();
    }

    // ==================== VehicleInstanceDto Factory ====================

    public static VehicleInstanceDto createVehicleInstance() {
        return Instancio.of(VehicleInstanceDto.class).create();
    }

    public static VehicleInstanceDto createVehicleInstanceWithChassisNumber(String chassisNumber) {
        return Instancio.of(VehicleInstanceDto.class)
                .set(field(VehicleInstanceDto::getChassisNumber), chassisNumber)
                .create();
    }

    // ==================== ExternalIdInfoDto Factory ====================

    public static ExternalIdInfoDto createExternalIdInfo(String ownerReference) {
        return Instancio.of(ExternalIdInfoDto.class)
                .set(field(ExternalIdInfoDto::getOwnerReference), ownerReference)
                .create();
    }

    public static ExternalIdInfoDto createExternalIdInfoFull(
            String ownerReference,
            String dealerReference,
            String channelReference) {
        return Instancio.of(ExternalIdInfoDto.class)
                .set(field(ExternalIdInfoDto::getOwnerReference), ownerReference)
                .set(field(ExternalIdInfoDto::getDealerReference), dealerReference)
                .set(field(ExternalIdInfoDto::getChannelReference), channelReference)
                .create();
    }
}