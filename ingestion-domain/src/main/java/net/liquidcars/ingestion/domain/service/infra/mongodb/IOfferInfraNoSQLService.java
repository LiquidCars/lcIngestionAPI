package net.liquidcars.ingestion.domain.service.infra.mongodb;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;

import java.util.List;
import java.util.UUID;

public interface IOfferInfraNoSQLService {

    void processOffer(OfferDto offer);

    void purgeObsoleteOffers(int daysOld);

    long countOffersFromJobId(UUID jobId);

    long countOffersFromReportId(UUID ingestionReportId);

    void promoteDraftOffersToVehicleOffers(UUID jobIdentifier, IngestionDumpType dumpType, UUID inventoryId, List<String> externalIdsToDelete, List<UUID> activeBookedOfferIds);

    void deleteDraftOffersByIngestionReportId(UUID jobIdentifier);
}
