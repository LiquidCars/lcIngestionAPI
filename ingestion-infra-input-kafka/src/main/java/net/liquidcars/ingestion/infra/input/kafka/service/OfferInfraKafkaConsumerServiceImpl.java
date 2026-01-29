package net.liquidcars.ingestion.infra.input.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraKafkaConsumerServiceImpl implements IOfferInfraKafkaConsumerService {

    private final IOfferInfraNoSQLService offerInfraNoSQLService;
    private final IOfferInfraSQLService offerInfraSQLService;

    @Override
    public void processOfferSave(OfferDto offerDto) {
        try{
            offerInfraNoSQLService.save(offerDto);
            log.info("Offer guardada en Mongo");
            offerInfraSQLService.save(offerDto);
            log.info("Offer guardada en PostgreSQL");
        }
        catch(Exception e){
            log.error("Error al guardar la oferta:{}", e.getMessage());
        }
    }
}
