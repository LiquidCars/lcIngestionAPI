package net.liquidcars.ingestion.domain.service.application;

import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IOfferIngestionProcessService {

    void processOffers(IngestionPayloadDto ingestionPayloadDto, UUID inventoryId, UUID requesterParticipantId, IngestionDumpType dumpType, String externalPublicationId);
    void processOffersFromUrl(IngestionFormat format, URI url, UUID inventoryId, UUID requesterParticipantId, IngestionDumpType dumpType, OffsetDateTime publicationDate, String externalPublicationId);
    void processOffersStream(IngestionFormat format, InputStream inputStream, UUID inventoryId, UUID requesterParticipantId, IngestionDumpType dumpType, OffsetDateTime publicationDate, String externalPublicationId);

    void syncPendingBatchReports();

    void processIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto);

    void syncPendingReports();

    void processIngestionReport(IngestionReportDto ingestionReportDto);

    void promoteDraftOffersToVehicleOffers(UUID ingestionReportId);

    void deleteDraftOffersByIngestionReportId(UUID ingestionReportId);

    List<IngestionReportDto> findIngestionReports();

    IngestionReportDto findIngestionReportById(UUID ingestionReportId);
}
