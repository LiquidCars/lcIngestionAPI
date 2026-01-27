package net.liquidcars.ingestion.application.service;

import net.liquidcars.ingestion.domain.model.Offer;
import net.liquidcars.ingestion.domain.port.output.OfferEventPublisher;
import net.liquidcars.ingestion.domain.port.output.OfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Application service for offer ingestion orchestration.
 * Coordinates between domain logic and infrastructure adapters.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OfferIngestionService {
    
    private final OfferRepository offerRepository;
    private final OfferEventPublisher eventPublisher;
    
    /**
     * Ingest a single offer
     */
    @Transactional
    public Offer ingestOffer(Offer offer) {
        log.debug("Ingesting offer with external ID: {}", offer.getExternalId());
        
        // Validate offer
        if (!offer.isValid()) {
            log.warn("Invalid offer data for external ID: {}", offer.getExternalId());
            throw new IllegalArgumentException("Invalid offer data");
        }
        
        // Check if offer already exists
        boolean exists = offerRepository.existsByExternalId(offer.getExternalId());
        
        // Set timestamps
        if (offer.getCreatedAt() == null) {
            offer.setCreatedAt(LocalDateTime.now());
        }
        offer.setUpdatedAt(LocalDateTime.now());
        
        // Save offer
        Offer savedOffer = offerRepository.save(offer);
        
        // Publish event
        if (exists) {
            eventPublisher.publishOfferUpdated(savedOffer);
            log.info("Offer updated: {}", savedOffer.getExternalId());
        } else {
            eventPublisher.publishOfferCreated(savedOffer);
            log.info("Offer created: {}", savedOffer.getExternalId());
        }
        
        return savedOffer;
    }
    
    /**
     * Ingest multiple offers in batch
     */
    @Transactional
    public List<Offer> ingestOfferBatch(List<Offer> offers) {
        log.info("Ingesting batch of {} offers", offers.size());
        
        // Validate all offers
        offers.forEach(offer -> {
            if (!offer.isValid()) {
                throw new IllegalArgumentException(
                    "Invalid offer data for external ID: " + offer.getExternalId()
                );
            }
        });
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        offers.forEach(offer -> {
            if (offer.getCreatedAt() == null) {
                offer.setCreatedAt(now);
            }
            offer.setUpdatedAt(now);
        });
        
        // Save all offers
        List<Offer> savedOffers = offerRepository.saveAll(offers);
        
        // Publish events (could be optimized with batch publishing)
        savedOffers.forEach(eventPublisher::publishOfferCreated);
        
        log.info("Successfully ingested {} offers", savedOffers.size());
        return savedOffers;
    }
}
