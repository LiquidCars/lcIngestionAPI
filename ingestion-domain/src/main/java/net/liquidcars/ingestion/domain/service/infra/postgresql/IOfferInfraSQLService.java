package net.liquidcars.ingestion.domain.service.infra.postgresql;

import net.liquidcars.ingestion.domain.model.AgreementDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.VehicleOfferDto;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IOfferInfraSQLService {
    @Transactional
    long deleteOffersByInventoryId(UUID inventoryId);

    @Transactional
    long deleteOffersByInventoryIdExcludingIds(UUID inventoryId, List<UUID> idsToKeep);

    @Transactional
    long deleteOffersByInventoryIdAndReferences(UUID inventoryId, List<String> externalReferences);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    List<UUID> processBatch(List<VehicleOfferDto> offers, List<UUID> activeBookedOfferIds);

    List<UUID> findActiveBookedOfferIds(UUID inventoryId);

    Set<String> findExternalRefsByOfferIds(List<UUID> offerIds);

    List<AgreementDto> findAgreementsByInventoryId(UUID inventoryId);

}
