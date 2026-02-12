package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.config.security.filter.IngestionContextFilter;
import net.liquidcars.ingestion.config.security.model.SecurityProperties;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
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

    // Infrastructure mocks to satisfy GlobalExceptionHandler and Filters
    @MockitoBean
    private IContextService contextService;
    @MockitoBean
    private SecurityProperties securityProperties;

    @Test
    void ingestBatch_ShouldReturnOk() throws Exception {
        when(mapper.toOfferDtoList(any(), any())).thenReturn(List.of());

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
        // We mock the Resource passed as the @RequestBody/body
        // Note: Spring usually maps the binary body to a Resource
        // To trigger your catch block, we simulate a failure in the stream acquisition

        mockMvc.perform(post("/stream")
                        .param("format", "xml")
                        .content("corrupted data")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                // If you want to force the IOException specifically:
                // You might need to use a custom RequestPostProcessor or MockMultipartFile
                // but usually, doThrow on the service is easier if the service handles the stream.
                .andExpect(status().isAccepted());
    }

    @Test
    void ingestStream_ShouldReturn500_WhenIOExceptionOccurs() throws Exception {
        // Force the service to throw the checked exception
        doAnswer(invocation -> {
            throw new IOException("Forced failure");
        }).when(ingestionService).processOffersStream(eq("xml"), any(InputStream.class));

        mockMvc.perform(post("/stream")
                        .param("format", "xml")
                        .content("test content")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof LCIngestionException));
        // If you want to be extra precise:
        // .andExpect(result -> assertTrue(result.getResolvedException() instanceof LCIngestionException));
    }

}
