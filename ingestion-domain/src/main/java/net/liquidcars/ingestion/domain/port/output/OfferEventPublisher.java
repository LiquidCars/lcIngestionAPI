package net.liquidcars.ingestion.domain.port.output;

import net.liquidcars.ingestion.domain.model.Offer;

/**
 * Output port (messaging interface) for publishing offer events.
 * This is implemented by infrastructure layer adapters (e.g., Kafka).
 */
public interface OfferEventPublisher {
    
    /**
     * Publish an offer created event
     */
    void publishOfferCreated(Offer offer);
    
    /**
     * Publish an offer updated event
     */
    void publishOfferUpdated(Offer offer);
    
    /**
     * Publish an offer deleted event
     */
    void publishOfferDeleted(String offerId);
}
