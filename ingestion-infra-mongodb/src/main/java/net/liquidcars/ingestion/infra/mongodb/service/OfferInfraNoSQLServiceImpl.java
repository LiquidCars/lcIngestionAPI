package net.liquidcars.ingestion.infra.mongodb.service;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.infra.mongodb.entity.OfferNoSQLEntity;
import net.liquidcars.ingestion.infra.mongodb.repository.OfferNoSqlRepository;
import net.liquidcars.ingestion.infra.mongodb.service.mapper.OfferInfraNoSQLMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferInfraNoSQLServiceImpl implements IOfferInfraNoSQLService {

    private final OfferNoSqlRepository repository;
    private final OfferInfraNoSQLMapper offerInfraNoSQLMapper;

    @Override
    public OfferDto save(OfferDto offer) {
        // 1. Dominio -> Entidad NoSQL
        OfferNoSQLEntity entity = offerInfraNoSQLMapper.toEntity(offer);

        // 2. Persistencia
        OfferNoSQLEntity savedEntity = repository.save(entity);

        // 3. Entidad NoSQL -> Dominio
        return offerInfraNoSQLMapper.toDto(savedEntity);
    }

}
