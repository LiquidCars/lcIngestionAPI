package net.liquidcars.ingestion.infra.input.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
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
        try {
            offerInfraNoSQLService.processOffer(offerDto);
        } catch (Exception e) {
            log.error("Critical failure in MongoDB for offer {}: {}", offerDto.getExternalId(), e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL persistence failed for offer: " + offerDto.getExternalId())
                    .cause(e)
                    .build();
        }
        try {
            offerInfraSQLService.processOffer(offerDto);
        } catch (Exception e) {
            log.error("Critical failure in Postgres for offer {}: {}", offerDto.getExternalId(), e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence failed for offer: " + offerDto.getExternalId())
                    .cause(e)
                    .build();
        }
    }
}
