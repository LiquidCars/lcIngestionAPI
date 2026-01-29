package net.liquidcars.ingestion.application.service.batch;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OfferItemWriter implements ItemWriter<OfferDto> {

    private final IOfferInfraKafkaProducerService kafkaProducer;

    @Override
    public void write(Chunk<? extends OfferDto> chunk) {
        chunk.forEach(kafkaProducer::sendOffer);
    }
}