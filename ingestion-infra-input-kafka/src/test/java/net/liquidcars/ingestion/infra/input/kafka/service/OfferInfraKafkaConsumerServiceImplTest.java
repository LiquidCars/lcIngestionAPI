package net.liquidcars.ingestion.infra.input.kafka.service;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.infra.mongodb.IOfferInfraNoSQLService;
import net.liquidcars.ingestion.domain.service.infra.postgresql.IOfferInfraSQLService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferInfraKafkaConsumerServiceImplTest {

    @InjectMocks
    private OfferInfraKafkaConsumerServiceImpl service;

    @Mock
    private IOfferInfraNoSQLService offerInfraNoSQLService;

    @Mock
    private IOfferInfraSQLService offerInfraSQLService;

    @Test
    @DisplayName("Should save to both repositories when everything goes well")
    void processOfferSave_ShouldSaveInBothSystems() {
        OfferDto offer = OfferDtoFactory.getOfferDto();

        service.processOfferSave(offer);

        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(offerInfraSQLService, times(1)).processOffer(offer);
    }

    @Test
    @DisplayName("Should attempt to save to SQL even if NoSQL fails")
    void processOfferSave_ShouldSaveInSQL_EvenIfNoSQLFails() {
        OfferDto offer = new OfferDto();
        doThrow(new RuntimeException("Mongo Down")).when(offerInfraNoSQLService).processOffer(any());

        service.processOfferSave(offer);

        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(offerInfraSQLService, times(1)).processOffer(offer);
    }

    @Test
    @DisplayName("Should attempt to save to NoSQL even if SQL fails")
    void processOfferSave_ShouldSaveInNoSQL_EvenIfSQLFails() {
        OfferDto offer = new OfferDto();
        doThrow(new RuntimeException("Postgres Down")).when(offerInfraSQLService).processOffer(any());

        service.processOfferSave(offer);

        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(offerInfraSQLService, times(1)).processOffer(offer);
    }
}
