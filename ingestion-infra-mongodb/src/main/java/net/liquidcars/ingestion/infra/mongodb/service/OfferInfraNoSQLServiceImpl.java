package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.OfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraNoSQLServiceImpl implements IOfferInfraNoSQLService {

    private final OfferNoSqlRepository repository;
    private final OfferInfraNoSQLMapper offerInfraNoSQLMapper;

    @Override
    @Transactional
    public void processOffer(OfferDto offer) {
        log.info("Processing NoSQL persistence for d: {}", offer.getId());

        try {
            OfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);
            entity.setCreatedAt(Instant.now());
            repository.findById(offer.getId().toString())
                    .ifPresentOrElse(
                            existingOffer -> updateIfNewer(existingOffer, entity),
                            () -> repository.save(entity)
                    );

        } catch (Exception e) {
            log.error("Failed to persist offer in NoSQL database. ID: {}", offer.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL persistence error for id: " + offer.getId())
                    .cause(e)
                    .build();
        }
    }

    private void updateIfNewer(OfferNoSQLEntity existing, OfferNoSQLEntity incoming) {
        boolean shouldUpdate = existing.getCreatedAt() == null ||
                incoming.getCreatedAt().isAfter(existing.getCreatedAt());

        if (shouldUpdate) {
            log.debug("Updating existing offer. ID: {}", incoming.getId());
            incoming.setId(existing.getId());
            if (existing.getCreatedAt() != null) {
                incoming.setCreatedAt(existing.getCreatedAt());
            }
            repository.save(incoming);
        } else {
            log.debug("Incoming offer is older than existing one. Skipping update. ExternalID: {}", incoming.getId());
        }
    }
}
