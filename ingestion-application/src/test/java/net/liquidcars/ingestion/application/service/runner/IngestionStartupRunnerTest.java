package net.liquidcars.ingestion.application.service.runner;

import net.liquidcars.ingestion.domain.model.batch.*;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IngestionStartupRunnerTest {

    @Mock
    private IOfferIngestionProcessService ingestionProcessService;

    @InjectMocks
    private IngestionStartupRunner startupRunner;

    @Test
    @DisplayName("onApplicationReady: Debería iniciar el proceso si el recurso existe")
    void onApplicationReady_Success() throws Exception {
        // Interceptamos la creación del recurso interno
        try (var mockedResource = mockConstruction(org.springframework.core.io.ClassPathResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenReturn(true);
                    when(mock.getInputStream()).thenReturn(InputStream.nullInputStream());
                })) {

            UUID expectedInventoryId = UUID.fromString("856b7d9a-cabd-4e86-aaca-b7a9641a9d0b");
            UUID expectedParticipantId = UUID.fromString("11111111-1111-1111-1111-111111111111");

            startupRunner.onApplicationReady();

            verify(ingestionProcessService).processOffersStream(
                    eq(IngestionFormat.xml),
                    any(Resource.class),
                    eq(expectedInventoryId),
                    eq(expectedParticipantId),
                    eq(IngestionDumpType.REPLACEMENT),
                    any(OffsetDateTime.class),
                    eq(IngestionStartupRunner.EXTERNAL_PUBLICATION_ID)
            );
        }
    }

    @Test
    @DisplayName("onApplicationReady: Debería loguear warn y salir si el fichero NO existe")
    void onApplicationReady_FileNotFound() throws Exception {
        try (var mockedResource = mockConstruction(org.springframework.core.io.ClassPathResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenReturn(false);
                })) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, never()).processOffersStream(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("onApplicationReady: Should enter catch if an exception occurs during resource check")
    void onApplicationReady_ExceptionCatch() throws Exception {
        try (var mockedResource = mockConstruction(org.springframework.core.io.ClassPathResource.class,
                (mock, context) -> {
                    when(mock.exists()).thenThrow(new RuntimeException("Simulated error"));
                })) {

            startupRunner.onApplicationReady();

            verify(ingestionProcessService, never()).processOffersStream(any(), any(), any(), any(), any(), any(), any());
        }
    }
}
