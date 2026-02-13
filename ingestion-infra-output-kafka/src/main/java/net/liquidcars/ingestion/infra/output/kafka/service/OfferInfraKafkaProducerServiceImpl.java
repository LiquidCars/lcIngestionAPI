package net.liquidcars.ingestion.infra.output.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.OfferSummaryDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferSummaryMsg;
import net.liquidcars.ingestion.infra.output.kafka.producer.BatchIngestionReportKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.producer.IngestionReportKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.producer.OfferKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.model.BatchIngestionReportMsg;
import net.liquidcars.ingestion.infra.output.kafka.model.IngestionReportMsg;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import net.liquidcars.ingestion.infra.output.kafka.producer.OfferSummaryKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.service.mapper.OfferInfraKafkaProducerMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferInfraKafkaProducerServiceImpl implements IOfferInfraKafkaProducerService {

    private final OfferInfraKafkaProducerMapper offerInfraKafkaProducerMapper;
    private final OfferKafkaPublisher offerKafkaPublisher;
    private final BatchIngestionReportKafkaPublisher batchIngestionReportKafkaPublisher;
    private final IngestionReportKafkaPublisher ingestionReportKafkaPublisher;
    private final OfferSummaryKafkaPublisher offerSummaryKafkaPublisher;

    @Override
    public void sendOffer(OfferDto offer) {
        try {
            log.debug("Sending offer with id: {}", offer.getId());
            OfferMsg offerMsg = offerInfraKafkaProducerMapper.toOfferMsg(offer);
            offerKafkaPublisher.sendOffer(offerMsg);
        } catch (Exception e) {
            log.error("Failed to dispatch offer to Kafka topic. OfferId: {}", offer.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Infrastructure failure: Kafka publisher is unavailable")
                    .cause(e)
                    .build();
        }
    }

    @Override
    public void sendBatchIngestionJobReport(IngestionBatchReportDto ingestionBatchReportDto) {
        try {
            log.debug("Sending batch report for Job: {}", ingestionBatchReportDto.getJobId());
            BatchIngestionReportMsg batchIngestionReportMsg = offerInfraKafkaProducerMapper.toBatchIngestionReportMsg(ingestionBatchReportDto);
            batchIngestionReportKafkaPublisher.sendBatchIngestionReport(batchIngestionReportMsg);
        } catch (Exception e) {
            log.error("Failed to dispatch batch report to Kafka topic. JobId: {}", ingestionBatchReportDto.getJobId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Infrastructure failure: Kafka publisher is unavailable")
                    .cause(e)
                    .build();
        }
    }

    @Override
    public void sendIngestionJobReport(IngestionReportDto ingestionReportDto) {
        try {
            log.debug("Sending ingestion report for Job: {}", ingestionReportDto.getId());
            IngestionReportMsg ingestionReportMsg = offerInfraKafkaProducerMapper.toIngestionReportMsg(ingestionReportDto);
            ingestionReportKafkaPublisher.sendIngestionReport(ingestionReportMsg);
        } catch (Exception e) {
            log.error("Failed to dispatch ingestion report to Kafka topic. JobId: {}", ingestionReportDto.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Infrastructure failure: Kafka publisher is unavailable")
                    .cause(e)
                    .build();
        }
    }

    @Override
    public void sendSavedNotification(OfferSummaryDto offerSummary) {
        try {
            log.debug("Saved offer with id: {}", offerSummary.getId());
            OfferSummaryMsg offerMsg = offerInfraKafkaProducerMapper.toOfferSummaryMsg(offerSummary);
            offerSummaryKafkaPublisher.sendSummaryOffer(offerMsg);
        } catch (Exception e) {
            log.error("Failed to dispatch offer to Kafka topic. OfferId: {}", offerSummary.getId(), e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.MESSAGING_BROKER_ERROR)
                    .message("Infrastructure failure: Kafka publisher is unavailable")
                    .cause(e)
                    .build();
        }
    }

}
