package net.liquidcars.ingestion.infra.input.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
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
        try {
            // 1. SQL Persistence: Primary source of truth.
            // If this fails, the transaction is marked for rollback immediately.
            offerInfraSQLService.processOffer(offerDto);
        } catch (Exception e) {
            log.error("Critical failure in Postgres for offer {}: {}", offerDto.getExternalId(), e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence failed for offer: " + offerDto.getExternalId())
                    .cause(e)
                    .build();
        }

        try {
            // 2. NoSQL Persistence: Secondary/Read-optimized source.
            // If this fails, it throws a RuntimeException, causing Postgres to rollback.
            offerInfraNoSQLService.processOffer(offerDto);
        } catch (Exception e) {
            log.error("Critical failure in MongoDB for offer {}: {}", offerDto.getExternalId(), e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL persistence failed for offer: " + offerDto.getExternalId())
                    .cause(e)
                    .build();
        }
    }

    /**
     * Processes and updates ingestion reports in both databases.
     * Following the same SQL-first strategy to maintain data integrity across platforms.
     */
    @Transactional
    @Override
    public void processIngestionReport(IngestionReportDto ingestionReportDto) {
        try {
            // Update RDBMS first as it usually manages the report's relational state.
            offerInfraSQLService.processIngestionReport(ingestionReportDto);
        } catch (Exception e) {
            log.error("Critical failure in Postgres for ingestion report {}: {}", ingestionReportDto.getJobId(), e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("SQL persistence failed for ingestion report: " + ingestionReportDto.getJobId())
                    .cause(e)
                    .build();
        }
        try {
            // Synchronize the report status in the NoSQL document store.
            offerInfraNoSQLService.processIngestionReport(ingestionReportDto);
        } catch (Exception e) {
            log.error("Critical failure in MongoDB for ingestion report {}: {}", ingestionReportDto.getJobId(), e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.DATABASE)
                    .message("NoSQL persistence failed for ingestion report: " + ingestionReportDto.getJobId())
                    .cause(e)
                    .build();
        }
    }
}
