package net.liquidcars.ingestion.infra.input.rest.handler;

import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.model.security.LCContext;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private IContextService contextService;

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @RestController
    static class ExceptionTestController {
        @GetMapping("/test-lc-exception")
        public void throwLcException() {
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.INVALID_REQUEST)
                    .message("Custom error message")
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionTestController())
                .setControllerAdvice(globalExceptionHandler)
                // Forzamos el conversor de JSON para asegurar que el body sea un objeto
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("handleLCException - Should return error response with participantId when context exists")
    void handleLCException_WithContext() throws Exception {
        String participantId = UUID.randomUUID().toString();
        LCContext mockContext = mock(LCContext.class);
        when(mockContext.getParticipantId()).thenReturn(participantId);
        when(contextService.getContext()).thenReturn(mockContext);

        mockMvc.perform(get("/test-lc-exception")
                        .accept(MediaType.APPLICATION_JSON)) // Importante aceptar JSON
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.uid").value(participantId)) // Esto ahora debería encontrar el objeto
                .andExpect(jsonPath("$.message").value("Custom error message"));
    }

    @Test
    @DisplayName("handleLCException - Should return error response with null uid when context is null")
    void handleLCException_WithoutContext() throws Exception {
        when(contextService.getContext()).thenReturn(null);

        mockMvc.perform(get("/test-lc-exception")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                // Si uid es null, JsonPath a veces prefiere comprobar existencia o valor nulo
                .andExpect(jsonPath("$.uid").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }


    @Test
    @DisplayName("handleValidation - Should return 400 when MethodArgumentNotValidException is thrown")
    void handleValidation_Success() throws Exception {
        // Para disparar MethodArgumentNotValidException de forma real necesitamos validaciones @Valid,
        // pero podemos probar el método del handler directamente para asegurar cobertura rápida.

        var response = globalExceptionHandler.handleValidation(null); // ex no se usa en tu lógica interna

        org.junit.jupiter.api.Assertions.assertEquals(400, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertEquals("Error input data", response.getBody().getMessage());
    }
}
