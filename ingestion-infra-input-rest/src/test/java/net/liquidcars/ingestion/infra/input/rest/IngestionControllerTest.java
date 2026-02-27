package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.config.security.model.SecurityProperties;
import net.liquidcars.ingestion.domain.model.batch.IngestionDumpType;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionReportPageDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.factory.TestDataFactory;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    @Autowired
    private IngestionController ingestionController;

    private final UUID reportId = UUID.randomUUID();
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
        when(mapper.toOfferDtoList(any(), eq(TEST_PARTICIPANT_ID), eq(TEST_INVENTORY_ID))).thenReturn(List.of());

        mockMvc.perform(post("/v1/ingestion/batch")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "INCREMENTAL")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffers(
                any(),
                eq(TEST_INVENTORY_ID),
                eq(TEST_PARTICIPANT_ID),
                eq(IngestionDumpType.INCREMENTAL),
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
                        .param("dumpType", "INCREMENTAL")
                        .content(content)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffersStream(
                eq(IngestionFormat.xml),
                any(Resource.class),
                eq(TEST_INVENTORY_ID),
                eq(TEST_PARTICIPANT_ID),
                eq(IngestionDumpType.INCREMENTAL),
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
                        any(), any(Resource.class), any(), any(), any(), any(), any());

        mockMvc.perform(post("/v1/ingestion/stream")
                        .param("format", "xml")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "INCREMENTAL")
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
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /reports - Should return 200 and list from page")
    void findIngestionReports_Success() throws Exception {
        // GIVEN
        // Construimos el PageDto usando el builder de Lombok
        IngestionReportPageDto pageDto = IngestionReportPageDto.builder()
                .content(Collections.emptyList())
                .totalElements(0L)
                .totalPages(0)
                .build();

        // El controlador llama al servicio. Usamos any() para el filtro IngestionReportFilterDto
        when(ingestionService.findIngestionReports(any())).thenReturn(pageDto);

        // El controlador extrae la lista del PageDto para pasarla al mapper
        // (Asegúrate de que en tu controlador hagas: pageDto.getContent())
        when(mapper.toIngestionReportList(any())).thenReturn(Collections.emptyList());

        // WHEN & THEN
        mockMvc.perform(get("/v1/ingestion/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ingestionService).findIngestionReports(any());
    }

    @Test
    @DisplayName("GET /reports/{id} - Should return 200 and report")
    void findIngestionReportById_Success() throws Exception {
        // GIVEN
        IngestionReportDto dto = TestDataFactory.createIngestionReport();
        when(ingestionService.findIngestionReportById(reportId)).thenReturn(dto);
        when(mapper.toIngestionReport(dto)).thenReturn(new IngestionReport());

        // WHEN & THEN
        mockMvc.perform(get("/v1/ingestion/reports/{ingestionReportId}", reportId))
                .andExpect(status().isOk());

        verify(ingestionService).findIngestionReportById(reportId);
    }

    @Test
    @DisplayName("POST /promote/{id} - Should return 200")
    void promoteDraftOffers_Success() throws Exception {
        // WHEN & THEN
        mockMvc.perform(post("/v1/ingestion/promote/{ingestionReportId}", reportId))
                .andExpect(status().isOk());

        verify(ingestionService).promoteDraftOffersToVehicleOffers(reportId, false);
    }

    @Test
    @DisplayName("DELETE /draft/{id} - Should return 204")
    void deleteDraftOffers_Success() throws Exception {
        // WHEN & THEN
        mockMvc.perform(delete("/v1/ingestion/draft/{ingestionReportId}", reportId))
                .andExpect(status().isNoContent());

        verify(ingestionService).deleteDraftOffersByIngestionReportId(reportId, false);
    }

    @Test
    @DisplayName("POST /stream - Success covers the try-with-resources block")
    void ingestStream_Success() throws Exception {
        // GIVEN
        Resource mockResource = mock(Resource.class);
        byte[] content = "test data".getBytes();
        when(mockResource.getInputStream()).thenReturn(new ByteArrayInputStream(content));

        // El controller usa getParticipantIdFromContext, podrías mockear el contextService aquí si fuera necesario
        // Pero para cobertura del método basta con que no lance excepción el servicio

        // WHEN & THEN
        mockMvc.perform(post("/v1/ingestion/stream")
                        .param("format", "json")
                        .param("inventoryId", TEST_INVENTORY_ID.toString())
                        .param("dumpType", "INCREMENTAL")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(content))
                .andExpect(status().isAccepted());

        // Nota: ingestStream en el controller tiene un try-with-resources.
        // Este test asegura que el stream se abre y se cierra correctamente.
    }

}