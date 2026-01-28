package net.liquidcars.ingestion.infra.input.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraKafkaConsumerServiceImpl implements IOfferInfraKafkaConsumerService {

    private final IOfferInfraNoSQLService offerInfraNoSQLService;

    @Override
    public void processOfferSave(OfferDto offerDto) {
        try{
            offerInfraNoSQLService.save(offerDto);
        }
        catch(Exception e){
            log.error("Error al guardar la oferta:{}", e.getMessage());
        }
    }
}
