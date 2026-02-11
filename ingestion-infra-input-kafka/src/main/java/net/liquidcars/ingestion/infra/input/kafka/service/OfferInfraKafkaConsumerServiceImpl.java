package net.liquidcars.ingestion.infra.input.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraKafkaConsumerServiceImpl implements IOfferInfraKafkaConsumerService {

    private final IOfferInfraNoSQLService offerInfraNoSQLService;
    private final IOfferInfraSQLService offerInfraSQLService;
    private final IOfferIngestionProcessService ingestionProcessService;

    /**
     * Persists the offer in both SQL (PostgreSQL) and NoSQL (MongoDB) databases.
     * * IMPORTANT: This method uses a single-resource Transaction Manager (Postgres).
     * By processing SQL first, we ensure that if Postgres fails, the NoSQL operation is never attempted.
     * If NoSQL fails, the LCIngestionException (RuntimeException) triggers a rollback in Postgres,
     * maintaining consistency between both data sources.
     */
    @Transactional
    @Override
    public void processOfferSave(OfferDto offerDto) {
        offerInfraSQLService.processOffer(offerDto);
        offerInfraNoSQLService.processOffer(offerDto);
    }

    /**
     * Processes ingestion batch report
     */
    @Override
    public void processIngestionReport(IngestionBatchReportDto ingestionBatchReportDto) {
        ingestionProcessService.processIngestionBatchReport(ingestionBatchReportDto);
    }
}
