package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
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
class OfferStreamItemReaderTest {

    @Mock
    private IOfferParserService parser;

    @Test
    void read_ShouldReturnAllOffersAndThenNull() throws Exception {
        JobDeleteExternalIdsCollector deleteExternalIdsCollector = new JobDeleteExternalIdsCollector();
        // Mock del parser para que produzca dos OfferDto
        doAnswer(inv -> {
            Consumer<OfferDto> action = inv.getArgument(1);
            action.accept(OfferDtoFactory.getOfferDto());
            action.accept(OfferDtoFactory.getOfferDto());
            return null;
        }).when(parser).parseAndProcess(any(InputStream.class), any(), any());

        OfferStreamItemReader reader = new OfferStreamItemReader();
        reader.start(parser, InputStream.nullInputStream(), deleteExternalIdsCollector);

        OfferDto first = reader.read();
        OfferDto second = reader.read();
        OfferDto third = reader.read();

        assertNotNull(first, "First offer should not be null");
        assertNotNull(second, "Second offer should not be null");
        assertNull(third, "Should return null when the parsing is finished");
    }

    @Test
    void read_ShouldThrowException_WhenParserFails() throws Exception {
        // Mock del parser para que lance excepción
        JobDeleteExternalIdsCollector deleteExternalIdsCollector = new JobDeleteExternalIdsCollector();
        doThrow(new RuntimeException("Crash!")).when(parser).parseAndProcess(any(InputStream.class), any(), any());

        OfferStreamItemReader reader = new OfferStreamItemReader();
        reader.start(parser, InputStream.nullInputStream(), deleteExternalIdsCollector);

        // Como el hilo es asíncrono, hay que esperar un poco hasta que la excepción esté en error
        Thread.sleep(100);

        RuntimeException thrown = assertThrows(RuntimeException.class, reader::read);
        assertTrue(thrown.getMessage().contains("Crash!"));
    }
}
