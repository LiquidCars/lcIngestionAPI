package net.liquidcars.ingestion.infra.input.kafka.consumer;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.input.kafka.IOfferInfraKafkaConsumerService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.OfferMsgFactory;
import net.liquidcars.ingestion.infra.input.kafka.service.mapper.OfferInfraKafkaConsumerMapper;
import net.liquidcars.ingestion.infra.output.kafka.model.OfferMsg;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferInfraKafkaConsumerTest {

    @InjectMocks
    private OfferInfraKafkaConsumer consumer;

    @Mock
    private OfferInfraKafkaConsumerMapper mapper;

    @Mock
    private IOfferInfraKafkaConsumerService service;

    @Test
    @DisplayName("Debe mapear y procesar la oferta cuando llega un mensaje de Kafka")
    void consumeOffer_ShouldMapAndProcessSuccessfully() {
        OfferMsg message = OfferMsgFactory.getOfferMsg();
        OfferDto mappedDto = OfferDtoFactory.getOfferDto();

        when(mapper.toOfferDto(message)).thenReturn(mappedDto);

        consumer.consumeOffer(message);

        verify(mapper, times(1)).toOfferDto(message);
        verify(service, times(1)).processOfferSave(mappedDto);
    }

    @Test
    @DisplayName("Debe capturar la excepción y loguear el error si falla el proceso")
    void consumeOffer_ShouldHandleException() {
        OfferMsg message = OfferMsgFactory.getOfferMsg();
        when(mapper.toOfferDto(any())).thenThrow(new RuntimeException("Mapping error"));

        consumer.consumeOffer(message);

        verify(service, never()).processOfferSave(any());
    }
}
