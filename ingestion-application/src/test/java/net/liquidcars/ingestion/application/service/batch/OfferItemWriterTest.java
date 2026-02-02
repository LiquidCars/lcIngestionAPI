package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.output.kafka.IOfferInfraKafkaProducerService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OfferItemWriterTest {

    @Mock
    private IOfferInfraKafkaProducerService kafkaProducer;

    @InjectMocks
    private OfferItemWriter writer;

    @Test
    void write_ShouldSendAllOffersInChunk() throws Exception {
        List<OfferDto> offers = List.of(OfferDtoFactory.getOfferDto(), OfferDtoFactory.getOfferDto(), OfferDtoFactory.getOfferDto());
        Chunk<OfferDto> chunk = new Chunk<>(offers);

        writer.write(chunk);

        verify(kafkaProducer, times(3)).sendOffer(any(OfferDto.class));
    }
}
