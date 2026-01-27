package net.liquidcars.ingestion.domain.port.output;

import net.liquidcars.ingestion.domain.model.Offer;

import java.util.List;
import java.util.Optional;

/**
 * Output port (repository interface) for Offer persistence.
 * This is implemented by infrastructure layer adapters.
 */
public interface OfferRepository {
    
    /**
     * Save an offer to the data store
     */
    Offer save(Offer offer);
    
    /**
     * Save multiple offers in batch
     */
    List<Offer> saveAll(List<Offer> offers);
    
    /**
     * Find an offer by its ID
     */
    Optional<Offer> findById(String id);
    
    /**
     * Find an offer by its external ID
     */
    Optional<Offer> findByExternalId(String externalId);
    
    /**
     * Check if an offer exists by external ID
     */
    boolean existsByExternalId(String externalId);
    
    /**
     * Delete an offer by ID
     */
    void deleteById(String id);
}
