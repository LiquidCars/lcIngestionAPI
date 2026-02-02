package net.liquidcars.ingestion.infra.postgresql.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.infra.postgresql.entity.OfferEntity;
import net.liquidcars.ingestion.infra.postgresql.repository.OfferSQLRepository;
import net.liquidcars.ingestion.infra.postgresql.service.mapper.OfferInfraSQLMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraSQLServiceImpl implements IOfferInfraSQLService {

    private final OfferSQLRepository sqlRepository;
    private final OfferInfraSQLMapper mapper;

    @Override
    public void processOffer(OfferDto offer) {
        log.info("Processing offer with externalId={}", offer.getExternalId());
        OfferEntity entity = mapper.toEntity(offer);
        sqlRepository.findByExternalId(offer.getExternalId())
                .ifPresentOrElse(
                        existingOffer -> {
                            if (entity.getCreatedAt().isAfter(existingOffer.getCreatedAt())) {
                                entity.setId(existingOffer.getId());
                                sqlRepository.save(entity);
                            }
                        },
                        () -> {
                            sqlRepository.save(entity);
                        }
                );
    }

}
