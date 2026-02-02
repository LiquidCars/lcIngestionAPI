package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
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
public class OfferStreamItemReaderTest {

    @Mock
    private IOfferParserService parser;

    @Test
    void read_ShouldReturnAllOffersAndThenNull() throws Exception {
        doAnswer(inv -> {
            Consumer<OfferDto> action = inv.getArgument(1);
            action.accept(OfferDtoFactory.getOfferDto());
            action.accept(OfferDtoFactory.getOfferDto());
            return null;
        }).when(parser).parseAndProcess(any(), any());

        OfferStreamItemReader reader = new OfferStreamItemReader(parser, InputStream.nullInputStream());

        assertNotNull(reader.read());
        assertNotNull(reader.read());
        assertNull(reader.read(), "Debe retornar null cuando el parseo termina");
    }

    @Test
    void read_ShouldThrowException_WhenParserFails() {
        doThrow(new RuntimeException("Crash!")).when(parser).parseAndProcess(any(), any());

        OfferStreamItemReader reader = new OfferStreamItemReader(parser, InputStream.nullInputStream());

        assertThrows(RuntimeException.class, () -> {
            for(int i=0; i<100; i++) {
                reader.read();
                Thread.sleep(10);
            }
        });
    }
}
