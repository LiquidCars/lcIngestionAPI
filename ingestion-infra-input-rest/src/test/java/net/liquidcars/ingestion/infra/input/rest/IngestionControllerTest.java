package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.config.security.model.SecurityProperties;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IngestionController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
public class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOfferIngestionProcessService ingestionService;

    @MockitoBean
    private IngestionControllerMapper mapper;

    @MockitoBean
    private IContextService contextService;

    @MockitoBean
    private SecurityProperties securityProperties;

    private static final UUID TEST_INVENTORY_ID = UUID.randomUUID();
    private static final UUID TEST_PARTICIPANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Configuramos el contexto para que getParticipantIdFromContext() no falle
        LCContext context = mock(LCContext.class);
        when(context.getParticipantId()).thenReturn(TEST_PARTICIPANT_ID.toString());
        when(contextService.getContext()).thenReturn(context);
    }

    @Test
    void ingestBatch_ShouldReturnAccepted() throws Exception {
        when(mapper.toOfferDtoList(any(), eq(TEST_PARTICIPANT_ID))).thenReturn(List.of());

        mockMvc.perform(post("/v1/ingestion/batch")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "UPDATE")
                        .content("[]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffers(
                any(),
                eq(TEST_INVENTORY_ID),
                eq(TEST_PARTICIPANT_ID),
                eq(IngestionDumpType.UPDATE),
                any()
        );
    }

    @Test
    void ingestFromUrl_ShouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/v1/ingestion/url")
                        .param("format", "json")
                        .param("url", "https://api.test.com/offers")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "REPLACEMENT"))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffersFromUrl(
                eq(IngestionFormat.json),
                any(URI.class),
                eq(TEST_INVENTORY_ID),
                eq(TEST_PARTICIPANT_ID),
                eq(IngestionDumpType.REPLACEMENT),
                any(),
                any()
        );
    }

    @Test
    void ingestStream_ShouldReturnAccepted() throws Exception {
        byte[] content = "<offers></offers>".getBytes();

        mockMvc.perform(post("/v1/ingestion/stream")
                        .param("format", "xml")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "UPDATE")
                        .content(content)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffersStream(
                eq(IngestionFormat.xml),
                any(InputStream.class),
                eq(TEST_INVENTORY_ID),
                eq(TEST_PARTICIPANT_ID),
                eq(IngestionDumpType.UPDATE),
                any(),
                any()
        );
    }

    @Test
    void ingestStream_ShouldReturnBadRequest_WhenServiceThrowsLCIngestionException() throws Exception {

        doThrow(LCIngestionException.builder()
                .techCause(LCTechCauseEnum.INVALID_REQUEST)
                .message("Forced failure")
                .build())
                .when(ingestionService)
                .processOffersStream(
                        any(), any(InputStream.class), any(), any(), any(), any(), any());

        mockMvc.perform(post("/v1/ingestion/stream")
                        .param("format", "xml")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "UPDATE")
                        .content("test content")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException() instanceof LCIngestionException)
                );
    }


    @Test
    void ingestBatch_ShouldReturn400_WhenRequiredParamsMissing() throws Exception {
        mockMvc.perform(post("/v1/ingestion/batch")
                        .content("[]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}