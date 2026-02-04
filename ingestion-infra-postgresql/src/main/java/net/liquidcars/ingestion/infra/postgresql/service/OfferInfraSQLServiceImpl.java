package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {

    private final OfferSQLRepository sqlRepository;
    private final OfferInfraSQLMapper mapper;

    @Override
    @Transactional // Vital to ensure the find + save is consistent
    public void processOffer(OfferDto offer) {
        log.info("Processing SQL persistence for externalId: {}", offer.getExternalId());

        try {
            OfferEntity entity = mapper.toEntity(offer);
            sqlRepository.findByExternalId(offer.getExternalId())
                    .ifPresentOrElse(
                            existingOffer -> updateIfNewer(existingOffer, entity),
                            () -> sqlRepository.save(entity)
                    );

        } catch (Exception e) {
            log.error("Failed to persist offer in SQL database. ExternalID: {}", offer.getExternalId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence error for externalId: " + offer.getExternalId())
                    .cause(e)
                    .build();
        }
    }

    private void updateIfNewer(OfferEntity existing, OfferEntity incoming) {
        if (incoming.getCreatedAt().isAfter(existing.getCreatedAt())) {
            log.debug("Updating existing offer. ExternalID: {}", incoming.getExternalId());
            incoming.setId(existing.getId());
            sqlRepository.save(incoming);
        } else {
            log.debug("Incoming offer is older than existing one. Skipping update. ExternalID: {}", incoming.getExternalId());
        }
    }
}
