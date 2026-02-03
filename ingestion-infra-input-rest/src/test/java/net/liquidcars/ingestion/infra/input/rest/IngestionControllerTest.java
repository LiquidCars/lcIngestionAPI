package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.IngestionApiApplication;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestionController.class)
@ContextConfiguration(classes = {IngestionApiApplication.class, IngestionController.class})
public class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IOfferIngestionProcessService ingestionService;

    @MockitoBean
    private IngestionControllerMapper mapper;

    @Test
    void ingestBatch_ShouldReturnOk() throws Exception {
        when(mapper.toOfferDtoList(any())).thenReturn(List.of());

        mockMvc.perform(post("/batch")
                        .content("[]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ingestionService).processOffers(any());
    }

    @Test
    void ingestFromUrl_ShouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/url")
                        .param("format", "json")
                        .param("url", "https://api.test.com/offers"))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffersFromUrl(eq("json"), any(URI.class));
    }

    @Test
    void ingestStream_ShouldReturnAccepted() throws Exception {

        byte[] content = "<offers></offers>".getBytes();

        mockMvc.perform(post("/stream")
                        .param("format", "xml")
                        .content(content)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffersStream(eq("xml"), any(InputStream.class));
    }

    @Test
    void ingestStream_ShouldReturnInternalServerError_WhenIOExceptionOccurs() throws Exception {
        org.springframework.core.io.Resource mockResource = mock(org.springframework.core.io.Resource.class);

        when(mockResource.getInputStream()).thenThrow(new java.io.IOException("Error de lectura simulado"));

        IngestionController controller = new IngestionController(ingestionService, mapper);

        org.springframework.http.ResponseEntity<Void> response =
                controller.ingestStream("xml", mockResource);

        org.junit.jupiter.api.Assertions.assertEquals(500, response.getStatusCode().value());
    }

    @Test
    void ingestStream_ShouldReturn500_WhenIOExceptionOccurs() throws IOException {
        IngestionController controller = new IngestionController(ingestionService, mapper);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getInputStream()).thenThrow(new IOException("Forced failure"));

        ResponseEntity<Void> response = controller.ingestStream("xml", mockResource);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }



}
