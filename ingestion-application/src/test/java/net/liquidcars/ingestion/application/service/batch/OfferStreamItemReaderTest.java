package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
public class OfferStreamItemReaderTest {

    @Mock
    private IOfferParserService parser;

    @Test
    void read_ShouldReturnAllOffersAndThenNull() throws Exception {
        // GIVEN: Un parser que añade dos ofertas y termina
        doAnswer(inv -> {
            Consumer<OfferDto> action = inv.getArgument(1);
            action.accept(new OfferDto());
            action.accept(new OfferDto());
            return null;
        }).when(parser).parseAndProcess(any(), any());

        OfferStreamItemReader reader = new OfferStreamItemReader(parser, InputStream.nullInputStream());

        // WHEN & THEN
        assertNotNull(reader.read());
        assertNotNull(reader.read());
        assertNull(reader.read(), "Debe retornar null cuando el parseo termina");
    }

    @Test
    void read_ShouldThrowException_WhenParserFails() {
        // GIVEN: Un parser que explota
        doThrow(new RuntimeException("Crash!")).when(parser).parseAndProcess(any(), any());

        OfferStreamItemReader reader = new OfferStreamItemReader(parser, InputStream.nullInputStream());

        // WHEN & THEN: Esperamos un poco a que el hilo virtual falle
        assertThrows(RuntimeException.class, () -> {
            for(int i=0; i<100; i++) { // Reintentos cortos por la naturaleza asíncrona
                reader.read();
                Thread.sleep(10);
            }
        });
    }
}
