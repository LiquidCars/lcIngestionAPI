package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionSkipListenerTest {

    @Mock
    private JobFailedIdsCollector failedIdsCollector;

    @InjectMocks
    private IngestionSkipListener skipListener;


    @Test
    @DisplayName("Debe capturar el ID cuando la excepción es LCIngestionParserException")
    void shouldAddIdToCollector_WhenLCIngestionParserExceptionOccurs() {
        ExternalIdInfoDto expectedId = TestDataFactory.createExternalIdInfo();
        LCIngestionParserException ex = TestDataFactory.createParserException(expectedId);

        skipListener.onSkipInRead(ex);

        verify(failedIdsCollector, times(1)).addId(expectedId);
    }

    @Test
    @DisplayName("No debe añadir ID si LCIngestionParserException tiene el identificador nulo")
    void shouldNotAddId_WhenParserExceptionHasNullId() {
        ExternalIdInfoDto nullId = null;

        LCIngestionParserException ex = TestDataFactory.createParserException(nullId);

        skipListener.onSkipInRead(ex);

        verifyNoInteractions(failedIdsCollector);
    }

    @Test
    @DisplayName("Debe loguear pero no añadir ID si es una LCIngestionException genérica")
    void shouldNotAddId_WhenGeneralLCIngestionExceptionOccurs() {
        LCIngestionException ex = new LCIngestionException(
                LCTechCauseEnum.CONVERSION_ERROR,
                "Error genérico de ingestión",
                new RuntimeException("Root cause")
        );

        // WHEN
        skipListener.onSkipInRead(ex);

        // THEN: Ahora sí, el primer bloque 'if' se ignora y no se llama al collector
        verifyNoInteractions(failedIdsCollector);
    }

    @Test
    @DisplayName("Debe manejar errores inesperados sin romper el flujo")
    void shouldHandleUnexpectedExceptions() {
        // GIVEN
        RuntimeException ex = new RuntimeException("Unexpected boom");

        // WHEN & THEN (No debe lanzar excepción)
        skipListener.onSkipInRead(ex);
        verifyNoInteractions(failedIdsCollector);
    }

    @Test
    @DisplayName("No debe añadir nada si el objeto no es del tipo esperado")
    void shouldNotAddId_WhenItemIsUnknownType() {
        // GIVEN
        String unknownItem = "Just a string";
        Throwable t = new RuntimeException("Error");

        // WHEN
        skipListener.onSkipInWrite(unknownItem, t);

        // THEN
        verifyNoInteractions(failedIdsCollector);
    }

    @Test
    @DisplayName("Debe loguear la incidencia en proceso (sin interactuar con el collector según código)")
    void shouldLogProcessSkip() {
        // GIVEN
        Object item = new Object();
        Throwable t = new RuntimeException("Validation error");

        // WHEN
        skipListener.onSkipInProcess(item, t);

        // THEN
        verifyNoInteractions(failedIdsCollector);
    }

    @Test
    @DisplayName("Debe capturar el ExternalIdInfo cuando el item es un OfferDto")
    void onSkipInWrite_WhenItemIsOfferDto_ShouldAddIdToCollector() {
        // GIVEN
        // 1. Creamos el ID que esperamos que el collector reciba
        ExternalIdInfoDto expectedId = TestDataFactory.createExternalIdInfo();

        // 2. Creamos el OfferDto (usando Instancio o Factory)
        OfferDto offer = TestDataFactory.createOfferDto();

        // 3. ¡IMPORTANTE! Inyectamos el ID esperado en el OfferDto
        // para que coincida con lo que el verify buscará después.
        offer.setExternalIdInfo(expectedId);

        Throwable error = new RuntimeException("Kafka Timeout");

        // WHEN
        skipListener.onSkipInWrite(offer, error);

        // THEN: Ahora sí coincidirán
        verify(failedIdsCollector, times(1)).addId(expectedId);
    }

    @Test
    @DisplayName("Debe loguear error pero no llamar al collector cuando el item es de tipo desconocido")
    void onSkipInWrite_WhenItemIsUnknown_ShouldNotInteractWithCollector() {
        // GIVEN
        Object unknownItem = "Un string cualquiera";
        Throwable error = new RuntimeException("Generic Error");

        // WHEN
        skipListener.onSkipInWrite(unknownItem, error);

        // THEN: Se ejecuta la rama 'else', por lo que no debe haber interacción
        verifyNoInteractions(failedIdsCollector);
    }

}
