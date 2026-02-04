package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.config.security.model.SecurityProperties;
import net.liquidcars.ingestion.domain.service.application.IOfferIngestionProcessService;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.mapper.IngestionControllerMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngestionController.class)
class IngestionControllerTest {

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

    @Test
    @WithMockUser(authorities = {"M2M_role"})
    @DisplayName("POST /batch - Success")
    void ingestBatch_ShouldReturnOk() throws Exception {
        when(mapper.toOfferDtoList(any())).thenReturn(List.of());

        mockMvc.perform(post("/batch")
                        .with(csrf())
                        .content("[]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(ingestionService).processOffers(any());
    }

    @Test
    @WithMockUser(authorities = {"LCSupport_role"})
    @DisplayName("POST /url - Success")
    void ingestFromUrl_ShouldReturnAccepted() throws Exception {
        mockMvc.perform(post("/url")
                        .with(csrf())
                        .param("format", "json")
                        .param("url", "https://api.test.com/offers"))
                .andExpect(status().isAccepted());

        verify(ingestionService).processOffersFromUrl(eq("json"), any(URI.class));
    }

    @Test
    @DisplayName("POST /batch - Unauthorized without user")
    void ingestBatch_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/batch")
                        .with(csrf())
                        .content("[]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}