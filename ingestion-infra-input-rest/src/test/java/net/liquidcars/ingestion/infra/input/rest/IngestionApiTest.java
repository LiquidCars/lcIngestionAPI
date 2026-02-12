package net.liquidcars.ingestion.infra.input.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngestionApiTest {

    private MockMvc mockMvc;
    private static final String TEST_INVENTORY_ID = UUID.randomUUID().toString();

    @RestController
    static class TestIngestionController implements IngestionApi {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestIngestionController()).build();
    }

    @Test
    void ingestBatch_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/batch")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestFromUrl_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/url")
                        .param("format", "json")
                        .param("url", "http://example.com")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "REPLACEMENT"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestStream_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/stream")
                        .param("format", "xml")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "UPDATE")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("binary-data"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestBatch_ShouldFailWhenInventoryIdIsMissing() throws Exception {
        // Al ser un parámetro @NotNull en la interfaz, el dispatcher fallará si no se envía
        mockMvc.perform(post("/batch")
                        .param("dumpType", "UPDATE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestBatch_ShouldFailWhenContentTypeIsMissing() throws Exception {
        mockMvc.perform(post("/batch")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "UPDATE")
                        .content("[]"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getRequest_ShouldReturnEmptyOptionalByDefault() {
        IngestionApi api = new TestIngestionController();

        java.util.Optional<org.springframework.web.context.request.NativeWebRequest> result = api.getRequest();

        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty(), "The default request should be Optional.empty()");
    }
}