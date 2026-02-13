package net.liquidcars.ingestion.infra.output.kafka.service;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.output.kafka.producer.OfferKafkaPublisher;
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
        OfferDto offerDto = OfferDtoFactory.getOfferDto();
        OfferMsg offerMsg = OfferMsgFactory.getOfferMsg();

        when(mapper.toOfferMsg(offerDto)).thenReturn(offerMsg);

        service.sendOffer(offerDto);

        verify(mapper, times(1)).toOfferMsg(offerDto);
        verify(publisher, times(1)).sendOffer(offerMsg);
    }
}
