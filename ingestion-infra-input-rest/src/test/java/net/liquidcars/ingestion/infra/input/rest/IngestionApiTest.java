package net.liquidcars.ingestion.infra.input.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngestionApiTest {

    private MockMvc mockMvc;

    // Creamos un controlador real para el test que use los métodos default
    @RestController
    static class TestIngestionController implements IngestionApi {}

    @BeforeEach
    void setUp() {
        // Inicializamos MockMvc con el controlador de test
        mockMvc = MockMvcBuilders.standaloneSetup(new TestIngestionController()).build();
    }

    @Test
    void ingestBatch_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isNotImplemented()); // Ahora sí devolverá 501
    }

    @Test
    void ingestFromUrl_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/url")
                        .param("format", "json")
                        .param("url", "http://example.com"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestStream_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/stream")
                        .param("format", "xml")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("binary-data"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestBatch_ShouldFailWhenContentTypeIsMissing() throws Exception {
        // Al no enviar Content-Type, MockMvc debería dar error de mapeo (415)
        // o 404 si el mapping es estricto.
        // Nota: StandaloneSetup es a veces laxo, si falla cámbialo a is4xxClientError()
        mockMvc.perform(post("/batch")
                        .content("[]"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getRequest_ShouldReturnEmptyOptionalByDefault() {
        // GIVEN
        IngestionApi api = new TestIngestionController();

        // WHEN
        java.util.Optional<org.springframework.web.context.request.NativeWebRequest> result = api.getRequest();

        // THEN
        org.junit.jupiter.api.Assertions.assertTrue(result.isEmpty(), "El request por defecto debe ser Optional.empty()");
    }
}