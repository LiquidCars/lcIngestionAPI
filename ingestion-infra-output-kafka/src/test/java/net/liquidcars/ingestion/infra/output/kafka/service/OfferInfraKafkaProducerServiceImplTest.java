package net.liquidcars.ingestion.infra.output.kafka.service;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.infra.output.kafka.client.OfferKafkaPublisher;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import net.liquidcars.ingestion.infra.output.kafka.service.mapper.OfferInfraKafkaProducerMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferInfraKafkaProducerServiceImplTest {

    @Mock
    private OfferInfraKafkaProducerMapper mapper;

    @Mock
    private OfferKafkaPublisher publisher;

    @InjectMocks
    private OfferInfraKafkaProducerServiceImpl service;

    @Test
    void sendOffer_ShouldMapAndPublish() {
        // GIVEN
        OfferDto offerDto = new OfferDto();
        OfferMsg offerMsg = new OfferMsg();

        when(mapper.toOfferMsg(offerDto)).thenReturn(offerMsg);

        // WHEN
        service.sendOffer(offerDto);

        // THEN
        verify(mapper, times(1)).toOfferMsg(offerDto);
        verify(publisher, times(1)).sendOffer(offerMsg);
    }
}
