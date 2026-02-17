package net.liquidcars.ingestion.application.service.batch;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import net.liquidcars.ingestion.factory.OfferDtoFactory;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

import java.io.InputStream;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferStreamItemReaderTest {

    @Mock
    private IOfferParserService parser;

    @Mock
    private StepExecution stepExecution;

    private OfferStreamItemReader reader;
    private JobDeleteExternalIdsCollector collector;

    @BeforeEach
    void setUp() {
        reader = new OfferStreamItemReader();
        collector = new JobDeleteExternalIdsCollector();
    }


    @Test
    @DisplayName("Debería registrar el colector en el contexto de ejecución antes del paso")
    void beforeStep_ShouldPutCollectorInContext() {
        // Given
        org.springframework.batch.core.JobExecution jobExecution = mock(org.springframework.batch.core.JobExecution.class);
        ExecutionContext executionContext = new ExecutionContext();

        when(stepExecution.getJobExecution()).thenReturn(jobExecution);
        when(jobExecution.getExecutionContext()).thenReturn(executionContext);

        // Inicializamos el collector llamando a start (o por reflexión)
        reader.start(parser, InputStream.nullInputStream(), collector);

        // When
        reader.beforeStep(stepExecution);

        // Then
        assertThat(executionContext.get("deleteExternalIdsCollector")).isEqualTo(collector);
    }

    @Test
    @DisplayName("Debería manejar el timeout de la cola y reintentar hasta que termine el parsing")
    void read_ShouldWaitAndEventuallyReturnNull_WhenParsingFinishesSlowly() throws Exception {
        // Mock que no hace nada inmediatamente
        doAnswer(inv -> {
            Thread.sleep(300); // Simulamos un parseo lento que no añade nada
            return null;
        }).when(parser).parseAndProcess(any(), any(), any());

        reader.start(parser, InputStream.nullInputStream(), collector);

        // Al llamar a read(), la cola estará vacía pero isParsingFinished será false inicialmente
        // El bucle while(true) con poll(200ms) debería ejecutarse
        OfferDto result = reader.read();

        assertNull(result);
    }

    @Test
    @DisplayName("Debería capturar LCIngestionException en el hilo del parser sin detener el proceso")
    void start_ShouldHandleLCIngestionExceptionInParserThread() throws Exception {
        // Given
        doThrow(TestDataFactory.createParserException(TestDataFactory.createExternalIdInfo())).when(parser)
                .parseAndProcess(any(), any(), any());

        // When
        reader.start(parser, InputStream.nullInputStream(), collector);

        // Esperamos a que el hilo virtual termine
        Thread.sleep(150);

        // Then: No debería haber fatalError
        assertDoesNotThrow(reader::read);
        assertNull(reader.read()); // Debería devolver null indicando fin sin crash
    }

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

    @Test
    @DisplayName("Debería lanzar LCIngestionException cuando se añade un error a la cola")
    void read_ShouldThrowLCIngestionException_WhenErrorIsAddedToQueue() throws Exception {
        // Given
        LCIngestionException expectedException = TestDataFactory.createParserException(TestDataFactory.createExternalIdInfo());

        // When
        reader.addErrorToQueue(expectedException);

        // Then
        // 1. Verificamos que al leer, se lanza la excepción que pusimos en la cola
        LCIngestionException thrown = assertThrows(LCIngestionException.class, () -> reader.read());
        assertThat(thrown).isEqualTo(expectedException);

        // 2. Verificamos el comportamiento de fin de proceso
        // CORRECCIÓN: Usamos doNothing() porque el método es void
        doNothing().when(parser).parseAndProcess(any(), any(), any());

        reader.start(parser, InputStream.nullInputStream(), collector);

        // Espera para que el hilo virtual ejecute el bloque finally { isParsingFinished = true; }
        Thread.sleep(100);

        assertThat(reader.read()).isNull();
    }
}
