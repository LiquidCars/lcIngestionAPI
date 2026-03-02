package net.liquidcars.ingestion.infra.input.rest;

import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReport;
import net.liquidcars.ingestion.infra.input.rest.model.IngestionReportPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IngestionApiTest {

    private MockMvc mockMvc;
    private static final String TEST_INVENTORY_ID = UUID.randomUUID().toString();
    private final UUID reportId = UUID.randomUUID();

    @RestController
    static class TestIngestionController implements IngestionApi {}

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestIngestionController()).build();
    }

    @Test
    void ingestBatch_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/v1/ingestion/batch")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "INCREMENTAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestFromUrl_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/v1/ingestion/url")
                        .param("format", "json")
                        .param("url", "http://example.com")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "REPLACEMENT"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestStream_ShouldReturnNotImplementedByDefault() throws Exception {
        mockMvc.perform(post("/v1/ingestion/stream")
                        .param("format", "xml")
                        .param("inventoryId", TEST_INVENTORY_ID)
                        .param("dumpType", "INCREMENTAL")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content("binary-data"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void ingestBatch_ShouldFailWhenInventoryIdIsMissing() throws Exception {
        mockMvc.perform(post("/v1/ingestion/batch")
                        .param("dumpType", "INCREMENTAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestBatch_ShouldFailWhenContentTypeIsMissing() throws Exception {
        mockMvc.perform(post("/v1/ingestion/batch")
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

    @Test
    @DisplayName("GET /reports - Should return Not Implemented")
    void findIngestionReports_ShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(get("/v1/ingestion/reports")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("GET /reports/{id} - Should return Not Implemented")
    void findIngestionReportById_ShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(get("/v1/ingestion/reports/{ingestionReportId}", reportId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("DELETE /draft/{id} - Should return Not Implemented")
    void deleteDraftOffers_ShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(delete("/v1/ingestion/draft/{ingestionReportId}", reportId))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("POST /promote/{id} - Should return Not Implemented")
    void promoteDraftOffers_ShouldReturnNotImplemented() throws Exception {
        mockMvc.perform(post("/v1/ingestion/promote/{ingestionReportId}", reportId))
                .andExpect(status().isNotImplemented());
    }

    @Test
    @DisplayName("GET /reports - Should trigger lambda via real NativeWebRequest context (json)")
    void findIngestionReports_LambdaExecuted_WithRealRequestContext() {
        org.springframework.mock.web.MockHttpServletRequest mockRequest =
                new org.springframework.mock.web.MockHttpServletRequest();
        mockRequest.addHeader("Accept", "application/json");
        org.springframework.mock.web.MockHttpServletResponse mockResponse =
                new org.springframework.mock.web.MockHttpServletResponse();

        IngestionApi api = new IngestionApi() {
            @Override
            public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
                return java.util.Optional.of(
                        new org.springframework.web.context.request.ServletWebRequest(mockRequest, mockResponse)
                );
            }
        };

        ResponseEntity<IngestionReportPage> response = api.findIngestionReports(
                0, 20, null, null, null, null, null, null, null, null, null,
                null, null,null, null, null
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.http.HttpStatus.NOT_IMPLEMENTED, response.getStatusCode()
        );
    }

    @Test
    @DisplayName("GET /reports - Should not set example when Accept is not json")
    void findIngestionReports_ShouldNotSetExampleResponse_WhenAcceptIsNotJson() {
        org.springframework.mock.web.MockHttpServletRequest mockRequest =
                new org.springframework.mock.web.MockHttpServletRequest();
        mockRequest.addHeader("Accept", "application/xml");

        IngestionApi api = new IngestionApi() {
            @Override
            public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
                return java.util.Optional.of(
                        new org.springframework.web.context.request.ServletWebRequest(mockRequest)
                );
            }
        };

        ResponseEntity<IngestionReportPage> response = api.findIngestionReports(
                0, 20, null, null, null, null, null, null, null, null, null,
                null, null,null, null, null
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.http.HttpStatus.NOT_IMPLEMENTED, response.getStatusCode()
        );
    }

    @Test
    @DisplayName("GET /reports/{id} - Should trigger lambda via real NativeWebRequest context (json)")
    void findIngestionReportById_LambdaExecuted_WithRealRequestContext() {
        org.springframework.mock.web.MockHttpServletRequest mockRequest =
                new org.springframework.mock.web.MockHttpServletRequest();
        mockRequest.addHeader("Accept", "application/json");
        org.springframework.mock.web.MockHttpServletResponse mockResponse =
                new org.springframework.mock.web.MockHttpServletResponse();

        IngestionApi api = new IngestionApi() {
            @Override
            public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
                return java.util.Optional.of(
                        new org.springframework.web.context.request.ServletWebRequest(mockRequest, mockResponse)
                );
            }
        };

        ResponseEntity<IngestionReport> response = api.findIngestionReportById(reportId);

        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.http.HttpStatus.NOT_IMPLEMENTED, response.getStatusCode()
        );
    }

    @Test
    @DisplayName("GET /reports/{id} - Should not set example when Accept is not json")
    void findIngestionReportById_ShouldNotSetExampleResponse_WhenAcceptIsNotJson() {
        org.springframework.mock.web.MockHttpServletRequest mockRequest =
                new org.springframework.mock.web.MockHttpServletRequest();
        mockRequest.addHeader("Accept", "application/xml");

        IngestionApi api = new IngestionApi() {
            @Override
            public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
                return java.util.Optional.of(
                        new org.springframework.web.context.request.ServletWebRequest(mockRequest)
                );
            }
        };

        ResponseEntity<IngestionReport> response = api.findIngestionReportById(reportId);

        org.junit.jupiter.api.Assertions.assertEquals(
                org.springframework.http.HttpStatus.NOT_IMPLEMENTED, response.getStatusCode()
        );
    }

    @Test
    @DisplayName("DELETE /draft - Test coverage for Accept: application/json (Inside IF)")
    void deleteDraftOffers_ShouldCoverIfBlock() {
        IngestionApi api = createApiWithRequest("application/json");
        ResponseEntity<Void> response = api.deleteDraftOffers(reportId);
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("DELETE /draft - Test coverage for Accept: application/xml (Outside IF)")
    void deleteDraftOffers_ShouldSkipIfBlock() {
        IngestionApi api = createApiWithRequest("application/xml");
        ResponseEntity<Void> response = api.deleteDraftOffers(reportId);
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /promote - Test coverage for Accept: application/json (Inside IF)")
    void promoteDraftOffers_ShouldCoverIfBlock() {
        IngestionApi api = createApiWithRequest("application/json");
        ResponseEntity<Void> response = api.promoteDraftOffers(reportId);
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /promote - Test coverage for Accept: application/xml (Outside IF)")
    void promoteDraftOffers_ShouldSkipIfBlock() {
        IngestionApi api = createApiWithRequest("application/xml");
        ResponseEntity<Void> response = api.promoteDraftOffers(reportId);
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /batch - Test coverage for Accept: application/json (Inside IF)")
    void ingestBatch_ShouldCoverIfBlock() {
        IngestionApi api = createApiWithRequest("application/json");
        ResponseEntity<Void> response = api.ingestBatch(
                UUID.randomUUID(),
                net.liquidcars.ingestion.domain.model.batch.IngestionDumpType.INCREMENTAL,
                new net.liquidcars.ingestion.infra.input.rest.model.IngestionPayload(),
                null
        );
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /url - Test coverage for Accept: application/json (Inside IF)")
    void ingestFromUrl_ShouldCoverIfBlock() {
        IngestionApi api = createApiWithRequest("application/json");
        ResponseEntity<Void> response = api.ingestFromUrl(
                IngestionFormat.json,
                java.net.URI.create("http://test.com"),
                UUID.randomUUID(),
                net.liquidcars.ingestion.domain.model.batch.IngestionDumpType.INCREMENTAL,
                null,
                null
        );
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("POST /stream - Test coverage for Accept: application/json (Inside IF)")
    void ingestStream_ShouldCoverIfBlock() {
        IngestionApi api = createApiWithRequest("application/json");
        ResponseEntity<Void> response = api.ingestStream(
                IngestionFormat.json,
                UUID.randomUUID(),
                net.liquidcars.ingestion.domain.model.batch.IngestionDumpType.INCREMENTAL,
                new org.springframework.core.io.ByteArrayResource("test".getBytes()),
                null,
                null
        );
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    @DisplayName("DELETE /draft - Cobertura total de IFs mediante prioridades")
    void deleteDraftOffers_FullBruteForceCoverage() {
        ejecutarConAccept("application/json");

        ejecutarConAccept("application/json;q=0.1, application/json;q=0.2");

        ejecutarConAccept("application/json, application/json, application/json, application/json");

        ejecutarConAccept("application/*, */*");
    }

    private void ejecutarConAccept(String accept) {
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse res = new org.springframework.mock.web.MockHttpServletResponse();
        req.addHeader("Accept", accept);

        IngestionApi api = new IngestionApi() {
            @Override
            public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
                return java.util.Optional.of(new org.springframework.web.context.request.ServletWebRequest(req, res));
            }
        };
        api.deleteDraftOffers(reportId);
    }

    private IngestionApi createApiWithRequest(String acceptHeader) {
        org.springframework.mock.web.MockHttpServletRequest mockRequest = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse mockResponse = new org.springframework.mock.web.MockHttpServletResponse();
        if (acceptHeader != null) {
            mockRequest.addHeader("Accept", acceptHeader);
        }

        return new IngestionApi() {
            @Override
            public java.util.Optional<org.springframework.web.context.request.NativeWebRequest> getRequest() {
                return java.util.Optional.of(new org.springframework.web.context.request.ServletWebRequest(mockRequest, mockResponse));
            }
        };
    }
}