package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.OfferDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface IOfferInfraSQLService {
    void processOfferWithinTransaction(OfferDto offer);

    @Transactional
    long deleteOffersByInventoryId(UUID inventoryId);

    @Transactional
    long deleteOffersByInventoryIdExcludingIds(UUID inventoryId, List<UUID> idsToKeep);

    @Transactional
    long deleteOffersByInventoryIdAndReferences(UUID inventoryId, List<String> externalReferences);

    void processOffer(OfferDto offer);
}
