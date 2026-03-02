package net.liquidcars.ingestion.application.service.runner;

import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionStartupRunnerTest {

    @Mock
    private IOfferIngestionProcessService ingestionProcessService;

    @InjectMocks
    private IngestionStartupRunner startupRunner;

    private static final UUID EXPECTED_INVENTORY_ID =
            UUID.fromString("856b7d9a-cabd-4e86-aaca-b7a9641a9d0b");

    private static final UUID EXPECTED_PARTICIPANT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    @DisplayName("onApplicationReady: debería iniciar el proceso si el recurso existe")
    void onApplicationReady_Success() {

        try (var mockedResource = mockConstruction(ClassPathResource.class,
                (mock, context) -> when(mock.exists()).thenReturn(true))) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService).processOffersStream(
                    eq(IngestionFormat.xml),
                    any(Resource.class),
                    eq(EXPECTED_INVENTORY_ID),
                    eq(EXPECTED_PARTICIPANT_ID),
                    eq(IngestionDumpType.REPLACEMENT),
                    any(OffsetDateTime.class),
                    eq(IngestionStartupRunner.EXTERNAL_PUBLICATION_ID)
            );
        }
    }

    @Test
    @DisplayName("onApplicationReady: debería loguear warn y salir si el fichero NO existe")
    void onApplicationReady_FileNotFound() {

        try (var mockedResource = mockConstruction(ClassPathResource.class,
                (mock, context) -> when(mock.exists()).thenReturn(false))) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, never())
                    .processOffersStream(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("onApplicationReady: debería entrar en el catch si ocurre una excepción")
    void onApplicationReady_ExceptionCatch() {

        try (var mockedResource = mockConstruction(ClassPathResource.class,
                (mock, context) -> when(mock.exists()).thenThrow(new RuntimeException("Boom")))) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, never())
                    .processOffersStream(any(), any(), any(), any(), any(), any(), any());
        }
    }
}