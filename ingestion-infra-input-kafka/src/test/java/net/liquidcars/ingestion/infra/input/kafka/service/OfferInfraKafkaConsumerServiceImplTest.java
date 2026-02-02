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
    @DisplayName("Debe guardar en ambos repositorios cuando todo va bien")
    void processOfferSave_ShouldSaveInBothSystems() {
        OfferDto offer = OfferDtoFactory.getOfferDto();

        service.processOfferSave(offer);

        // THEN
        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(offerInfraSQLService, times(1)).processOffer(offer);
    }

    @Test
    @DisplayName("Debe intentar guardar en SQL aunque NoSQL falle")
    void processOfferSave_ShouldSaveInSQL_EvenIfNoSQLFails() {
        // GIVEN
        OfferDto offer = new OfferDto();
        doThrow(new RuntimeException("Mongo Down")).when(offerInfraNoSQLService).processOffer(any());

        // WHEN
        service.processOfferSave(offer);

        // THEN
        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(offerInfraSQLService, times(1)).processOffer(offer); // Se ejecuta a pesar del error anterior
    }

    @Test
    @DisplayName("Debe intentar guardar en NoSQL aunque SQL falle")
    void processOfferSave_ShouldSaveInNoSQL_EvenIfSQLFails() {
        // GIVEN
        OfferDto offer = new OfferDto();
        doThrow(new RuntimeException("Postgres Down")).when(offerInfraSQLService).processOffer(any());

        service.processOfferSave(offer);

        // THEN
        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
        verify(offerInfraSQLService, times(1)).processOffer(offer);
    }
}
