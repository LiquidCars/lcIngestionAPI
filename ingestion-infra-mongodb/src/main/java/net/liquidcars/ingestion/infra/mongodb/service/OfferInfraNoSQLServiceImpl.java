package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.OfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraNoSQLServiceImpl implements IOfferInfraNoSQLService {

    private final OfferNoSqlRepository repository;
    private final OfferInfraNoSQLMapper offerInfraNoSQLMapper;

    @Override
    public void processOffer(OfferDto offer) {
        log.info("Processing offer with externalId={}", offer.getExternalId());
        OfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);
        repository.findByExternalId(offer.getExternalId())
                .ifPresentOrElse(
                        existingOffer -> {
                            if (entity.getCreatedAt().isAfter(existingOffer.getCreatedAt())) {
                                entity.setId(existingOffer.getId());
                                repository.save(entity);
                            }
                        },
                        () -> {
                            // Si no existe, se inserta directamente
                            repository.save(entity);
                        }
                );
    }

}
