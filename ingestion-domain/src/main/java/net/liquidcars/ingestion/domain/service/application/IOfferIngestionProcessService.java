package net.liquidcars.ingestion.domain.service.application;

import net.liquidcars.ingestion.domain.model.IngestionPayloadDto;
import net.liquidcars.ingestion.domain.model.batch.*;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IOfferIngestionProcessService {

    void processOffers(IngestionPayloadDto ingestionPayloadDto, UUID inventoryId, UUID requesterParticipantId, IngestionDumpType dumpType, String externalPublicationId);
    void processOffersFromUrl(IngestionFormat format, URI url, UUID inventoryId, UUID requesterParticipantId, IngestionDumpType dumpType, OffsetDateTime publicationDate, String externalPublicationId);
    void processOffersStream(IngestionFormat format, Resource resource, UUID inventoryId, UUID requesterParticipantId, IngestionDumpType dumpType, OffsetDateTime publicationDate, String externalPublicationId);

    void syncPendingBatchReports();

    void processIngestionBatchReport(IngestionBatchReportDto ingestionBatchReportDto);

    void syncPendingReports();

    void executeDeferredPromotions();

    void processIngestionReport(IngestionReportDto ingestionReportDto);

    void promoteDraftOffersToVehicleOffers(UUID ingestionReportId, boolean async);

    void deleteDraftOffersByIngestionReportId(UUID ingestionReportId, boolean async);

    IngestionReportPageDto findIngestionReports(IngestionReportFilterDto filter);

    IngestionReportDto findIngestionReportById(UUID ingestionReportId);
}
