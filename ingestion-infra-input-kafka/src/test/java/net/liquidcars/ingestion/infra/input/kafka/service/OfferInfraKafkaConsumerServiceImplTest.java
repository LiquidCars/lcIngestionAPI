package net.liquidcars.ingestion.infra.input.kafka.service;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
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
    @DisplayName("Should save to SQL and NoSQL when everything goes well")
    void processOfferSave_ShouldSaveInBothSystems() {
        OfferDto offer = OfferDtoFactory.getOfferDto();

        service.processOfferSave(offer);

        verify(offerInfraSQLService, times(1)).processOffer(offer);
        verify(offerInfraNoSQLService, times(1)).processOffer(offer);
    }

    @Test
    @DisplayName("Should NOT attempt NoSQL when SQL fails")
    void processOfferSave_ShouldNotSaveInNoSQL_WhenSQLFails() {
        OfferDto offer = OfferDtoFactory.getOfferDto();

        doThrow(LCIngestionException.builder()
                .techCause(LCTechCauseEnum.DATABASE)
                .message("SQL persistence error for id: " + offer.getId())
                .build())
                .when(offerInfraSQLService)
                .processOffer(any());

        assertThrows(LCIngestionException.class,
                () -> service.processOfferSave(offer));

        verify(offerInfraSQLService, times(1)).processOffer(any());
        verify(offerInfraNoSQLService, never()).processOffer(any());
    }

    @Test
    @DisplayName("Should attempt NoSQL after SQL success and fail if NoSQL fails")
    void processOfferSave_ShouldSaveInSQL_AndFailIfNoSQLFails() {
        OfferDto offer = OfferDtoFactory.getOfferDto();

        doThrow(LCIngestionException.builder()
                .techCause(LCTechCauseEnum.DATABASE)
                .message("SQL persistence error for id: " + offer.getId())
                .build())
                .when(offerInfraNoSQLService)
                .processOffer(any());

        assertThrows(LCIngestionException.class,
                () -> service.processOfferSave(offer));

        verify(offerInfraSQLService, times(1)).processOffer(any());
        verify(offerInfraNoSQLService, times(1)).processOffer(any());
    }

}
