package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.application.service.parser.model.JSON.*;
import net.liquidcars.ingestion.application.service.parser.model.XML.*;
import net.liquidcars.ingestion.domain.model.*;
import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReport;
import net.liquidcars.ingestion.infra.output.kafka.model.*;
import org.instancio.Instancio;

import java.math.BigDecimal;
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

    public static IngestionBatchReportDto createIngestionBatchReportDto() {
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

    public static VehicleInstanceDto createVehicleInstanceJSON() {
        return Instancio.of(VehicleInstanceDto.class).create();
    }

    public static VehicleInstanceDto createVehicleInstanceWithChassisNumber(String chassisNumber) {
        return Instancio.of(VehicleInstanceDto.class)
                .set(field(VehicleInstanceDto::getChassisNumber), chassisNumber)
                .create();
    }

    // ==================== VehicleModelJSONModel Factory ====================

    public static VehicleModelJSONModel createVehicleModelJSONModel() {
        return Instancio.of(VehicleModelJSONModel.class).create();
    }

    public static VehicleModelJSONModel createVehicleModelJSONModelWithData(Long id, String brand, String model, Integer cv, Double acc, KeyValueJSONModel bt) {
        return Instancio.of(VehicleModelJSONModel.class)
                .set(field(VehicleModelJSONModel::getId), id)
                .set(field(VehicleModelJSONModel::getBrand), brand)
                .set(field(VehicleModelJSONModel::getModel), model)
                .set(field(VehicleModelJSONModel::getCv), cv)
                .set(field(VehicleModelJSONModel::getAcceleration), acc)
                .set(field(VehicleModelJSONModel::getBodyType), bt)
                .create();
    }

    // ==================== VehicleModelMsg Factory ====================

    public static VehicleModelMsg createVehicleModelMsg() {
        return Instancio.of(VehicleModelMsg.class).create();
    }

    public static VehicleModelMsg createVehicleModelMsgWithData(Long id, String brand, String model, Integer cv, Double acc, KeyValueMsg bt) {
        return Instancio.of(VehicleModelMsg.class)
                .set(field(VehicleModelMsg::getId), id)
                .set(field(VehicleModelMsg::getBrand), brand)
                .set(field(VehicleModelMsg::getModel), model)
                .set(field(VehicleModelMsg::getCv), cv)
                .set(field(VehicleModelMsg::getAcceleration), acc)
                .set(field(VehicleModelMsg::getBodyType), bt)
                .create();
    }

    // ==================== VehicleModelXMLModel Factory ====================

    public static VehicleModelXMLModel createVehicleModelXMLModel() {
        return Instancio.of(VehicleModelXMLModel.class)
                .create();
    }

    public static VehicleModelXMLModel createVehicleModelXMLModelWithData(Long id, String brand, String model, Integer cv, Double acc, KeyValueXMLModel bt) {
        return Instancio.of(VehicleModelXMLModel.class)
                .set(field(VehicleModelXMLModel::getId), id)
                .set(field(VehicleModelXMLModel::getBrand), brand)
                .set(field(VehicleModelXMLModel::getModel), model)
                .set(field(VehicleModelXMLModel::getCv), cv)
                .set(field(VehicleModelXMLModel::getAcceleration), acc)
                .set(field(VehicleModelXMLModel::getBodyType), bt)
                .create();
    }

    // ==================== GPSLocationJSONModel Factory ====================

    public static GPSLocationJSONModel createGPSLocationJSONModel(String name, double latitude, double longitude) {
        return Instancio.of(GPSLocationJSONModel.class)
                .set(field(GPSLocationJSONModel::getName), name)
                .set(field(GPSLocationJSONModel::getLatitude), latitude)
                .set(field(GPSLocationJSONModel::getLongitude), longitude)
                .create();
    }

    // ==================== GPSLocationJSONModel Factory ====================

    public static GPSLocationMsg createGPSLocationMsg(String name, double latitude, double longitude) {
        return Instancio.of(GPSLocationMsg.class)
                .set(field(GPSLocationMsg::getName), name)
                .set(field(GPSLocationMsg::getLatitude), latitude)
                .set(field(GPSLocationMsg::getLongitude), longitude)
                .create();
    }

    // ==================== GPSLocationJSONModel Factory ====================

    public static GPSLocationXMLModel createGPSLocationXMLModel(String name, double latitude, double longitude) {
        return Instancio.of(GPSLocationXMLModel.class)
                .set(field(GPSLocationXMLModel::getName), name)
                .set(field(GPSLocationXMLModel::getLatitude), latitude)
                .set(field(GPSLocationXMLModel::getLongitude), longitude)
                .create();
    }

    // ==================== OfferJSONModel Factory ====================

    public static OfferJSONModel createValidOfferJSONModel() {
        return Instancio.of(OfferJSONModel.class)
                .set(field(OfferJSONModel::getSellerType), CarOfferSellerTypeEnumJSONModel.usedCar_ProfessionalSeller)
                .set(field(OfferJSONModel::getPrice), MoneyJSONModel.builder().amount(new BigDecimal("15000")).build())
                .generate(field(OfferJSONModel::getResources), gen -> gen.collection().minSize(1))
                .create();
    }

    // ==================== OfferXMLModel Factory ====================

    public static OfferXMLModel createValidOfferXMLModel() {
        return Instancio.of(OfferXMLModel.class)
                .set(field(OfferXMLModel::getSellerType), CarOfferSellerTypeEnumXMLModel.usedCar_ProfessionalSeller)
                .set(field(OfferXMLModel::getPrice), Instancio.of(MoneyXMLModel.class)
                        .set(field(MoneyXMLModel::getAmount), new BigDecimal("15000"))
                        .create())
                .generate(field(OfferXMLModel::getResources), gen -> gen.collection().minSize(1))
                .create();
    }

    // ==================== VehicleInstanceJSONModel Factory ====================

    public static VehicleInstanceJSONModel createVehicleInstanceJSON(long id, String plate, String chassis) {
        return Instancio.of(VehicleInstanceJSONModel.class)
                .set(field(VehicleInstanceJSONModel::getId), id)
                .set(field(VehicleInstanceJSONModel::getPlate), plate)
                .set(field(VehicleInstanceJSONModel::getChassisNumber), chassis)
                .generate(field(VehicleInstanceJSONModel::getEquipments), gen -> gen.collection().size(2))
                .create();
    }

    // En TestDataFactory.java
    public static VehicleInstanceJSONModel createVehicleInstanceJSONModelWithSameData(VehicleInstanceJSONModel base, long newId) {
        return Instancio.of(VehicleInstanceJSONModel.class)
                .set(field(VehicleInstanceJSONModel::getId), newId)
                .set(field(VehicleInstanceJSONModel::getPlate), base.getPlate())
                .set(field(VehicleInstanceJSONModel::getChassisNumber), base.getChassisNumber())
                .set(field(VehicleInstanceJSONModel::getMileage), base.getMileage())
                .set(field(VehicleInstanceJSONModel::getVehicleModel), base.getVehicleModel())
                .set(field(VehicleInstanceJSONModel::getColor), base.getColor())
                .set(field(VehicleInstanceJSONModel::getState), base.getState())
                .set(field(VehicleInstanceJSONModel::getRegistrationYear), base.getRegistrationYear())
                .set(field(VehicleInstanceJSONModel::getRegistrationMonth), base.getRegistrationMonth())
                .set(field(VehicleInstanceJSONModel::isMetallicPaint), base.isMetallicPaint())
                .create();
    }

// ==================== VehicleInstanceMsg Factory ====================

    public static VehicleInstanceMsg createVehicleInstanceMsg(long id, String plate, String chassis) {
        return Instancio.of(VehicleInstanceMsg.class)
                .set(field(VehicleInstanceMsg::getId), id)
                .set(field(VehicleInstanceMsg::getPlate), plate)
                .set(field(VehicleInstanceMsg::getChassisNumber), chassis)
                .generate(field(VehicleInstanceMsg::getEquipments), gen -> gen.collection().size(2))
                .create();
    }

    // En TestDataFactory.java
    public static VehicleInstanceMsg createVehicleInstanceMsgWithSameData(VehicleInstanceMsg base, long newId) {
        return Instancio.of(VehicleInstanceMsg.class)
                .set(field(VehicleInstanceMsg::getId), newId)
                .set(field(VehicleInstanceMsg::getPlate), base.getPlate())
                .set(field(VehicleInstanceMsg::getChassisNumber), base.getChassisNumber())
                .set(field(VehicleInstanceMsg::getMileage), base.getMileage())
                .set(field(VehicleInstanceMsg::getVehicleModel), base.getVehicleModel())
                .set(field(VehicleInstanceMsg::getColor), base.getColor())
                .set(field(VehicleInstanceMsg::getState), base.getState())
                .set(field(VehicleInstanceMsg::getRegistrationYear), base.getRegistrationYear())
                .set(field(VehicleInstanceMsg::getRegistrationMonth), base.getRegistrationMonth())
                .set(field(VehicleInstanceMsg::isMetallicPaint), base.isMetallicPaint())
                .create();
    }


    // ==================== LCIngestionParserException Factory ====================

    public static LCIngestionParserException createParserException(ExternalIdInfoDto id) {
        return new LCIngestionParserException(
                LCTechCauseEnum.CONVERSION_ERROR,
                "Error de parseo detectado",
                new RuntimeException("Root cause"),
                id
        );
    }

    // ==================== VehicleInstanceXMLModel Factory ====================

    public static VehicleInstanceXMLModel createVehicleInstanceXMLModel(long id, String plate, String chassis) {
        return Instancio.of(VehicleInstanceXMLModel.class)
                .set(field(VehicleInstanceXMLModel::getId), id)
                .set(field(VehicleInstanceXMLModel::getPlate), plate)
                .set(field(VehicleInstanceXMLModel::getChassisNumber), chassis)
                .generate(field(VehicleInstanceXMLModel::getEquipments), gen -> gen.collection().size(2))
                .create();
    }

    public static VehicleInstanceXMLModel createVehicleInstanceXMLModelWithSameData(VehicleInstanceXMLModel base, long newId) {
        return Instancio.of(VehicleInstanceXMLModel.class)
                .set(field(VehicleInstanceXMLModel::getId), newId)
                .set(field(VehicleInstanceXMLModel::getPlate), base.getPlate())
                .set(field(VehicleInstanceXMLModel::getChassisNumber), base.getChassisNumber())
                .set(field(VehicleInstanceXMLModel::getMileage), base.getMileage())
                .set(field(VehicleInstanceXMLModel::getVehicleModel), base.getVehicleModel())
                .set(field(VehicleInstanceXMLModel::getColor), base.getColor())
                .set(field(VehicleInstanceXMLModel::getState), base.getState())
                .set(field(VehicleInstanceXMLModel::getRegistrationYear), base.getRegistrationYear())
                .set(field(VehicleInstanceXMLModel::getRegistrationMonth), base.getRegistrationMonth())
                .set(field(VehicleInstanceXMLModel::isMetallicPaint), base.isMetallicPaint())
                .create();
    }

    // ==================== ExternalIdInfoDto Factory ====================

    public static ExternalIdInfoDto createExternalIdInfo() {
        return ExternalIdInfoDto.builder()
                .ownerReference("OWN-1")
                .dealerReference("DLR-2")
                .channelReference("CH-3")
                .build();
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

    // ==================== IngestionReport Factory ====================

    public static IngestionReport createRandomIngestionReport() {
        return org.instancio.Instancio.create(IngestionReport.class);
    }

    public static IngestionReport createIngestionReportWithLists(int failedSize, int deleteSize) {
        return org.instancio.Instancio.of(IngestionReport.class)
                .generate(org.instancio.Select.field(IngestionReport::getFailedExternalIds),
                        gen -> gen.collection().size(failedSize))
                .generate(org.instancio.Select.field(IngestionReport::getIdsForDelete),
                        gen -> gen.collection().size(deleteSize))
                .create();
    }

    // ==================== OfferSummaryMsg Factory ====================

    public static OfferSummaryMsg createOfferSummaryMsg() {
        return org.instancio.Instancio.create(OfferSummaryMsg.class);
    }

    // ==================== IngestionReportResponseActionMsg Factory ====================

    public static IngestionReportResponseActionMsg createIngestionReportResponseActionMsg() {
        return Instancio.of(IngestionReportResponseActionMsg.class)
                .generate(field(IngestionReportResponseActionMsg::getIngestionReportId), gen -> gen.string().length(10))
                .create();
    }

    // ==================== BatchIngestionReportMsg Factory ====================

    public static BatchIngestionReportMsg createBatchIngestionReportMsg() {
        return org.instancio.Instancio.create(BatchIngestionReportMsg.class);
    }

    // ==================== BatchIngestionReportMsg Factory ====================

    public static IngestionReportMsg createIngestionReportMsg() {
        return org.instancio.Instancio.create(IngestionReportMsg.class);
    }


    // ==================== OfferSummaryDto Factory ====================

    public static OfferSummaryDto createOfferSummaryDto() {
        return org.instancio.Instancio.create(OfferSummaryDto.class);
    }

    // ==================== IngestionReportResponseActionDto Factory ====================

    public static IngestionReportResponseActionDto createIngestionReportResponseActionDto() {
        return org.instancio.Instancio.create(IngestionReportResponseActionDto.class);
    }

    // ==================== IngestionReportResponseActionDto Factory ====================

    public static CarInstanceEquipmentDto createCarInstanceEquipmentDto() {
        return org.instancio.Instancio.create(CarInstanceEquipmentDto.class);
    }

    // ==================== VehicleInstanceDto Factory ====================

    public static VehicleInstanceDto createVehicleInstanceDto() {
        return org.instancio.Instancio.create(VehicleInstanceDto.class);
    }

    // ==================== VehicleModelDto Factory ====================

    public static VehicleModelDto createVehicleModelDto() {
        return org.instancio.Instancio.create(VehicleModelDto.class);
    }

    // ==================== VehicleModelDto Factory ====================

    public static PostalAddressDto createPostalAddressDto() {
        return org.instancio.Instancio.create(PostalAddressDto.class);
    }

    // ==================== VehicleModelDto Factory ====================

    public static ParticipantAddressDto createParticipantAddressDto() {
        return org.instancio.Instancio.create(ParticipantAddressDto.class);
    }
}