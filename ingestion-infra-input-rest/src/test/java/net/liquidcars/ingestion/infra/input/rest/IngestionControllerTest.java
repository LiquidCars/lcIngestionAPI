package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.IngestionApiApplication;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
