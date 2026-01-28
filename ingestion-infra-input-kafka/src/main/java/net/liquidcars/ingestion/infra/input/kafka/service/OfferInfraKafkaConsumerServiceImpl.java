package net.liquidcars.ingestion.infra.input.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraKafkaConsumerServiceImpl implements IOfferInfraKafkaConsumerService {
    @Override
    public void processOfferSave(OfferDto offerDto) {

    }
}
