package net.liquidcars.ingestion.application.service.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.JSON.OfferJSONModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferJSONProcessorTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @Mock
    private OfferParserMapper offerParserMapper;
    @Mock
    private OfferStreamItemReader offerReader;
    @Mock
    private JobDeleteExternalIdsCollector deleteCollector;

    @InjectMocks
    private OfferJSONProcessor offerJSONProcessor;

    @Test
    @DisplayName("Debe soportar solo formato JSON")
    void supports_JsonFormat() {
        assertThat(offerJSONProcessor.supports(IngestionFormat.json)).isTrue();
        assertThat(offerJSONProcessor.supports(IngestionFormat.xml)).isFalse();
    }

    @Test
    @DisplayName("Debe procesar ofertas y eliminaciones con éxito")
    void parseAndProcess_FullSuccess() throws Exception {
        String json = """
                {
                  "offers": [{ "ownerReference": "O1", "dealerReference": "D1" }],
                  "offersToDelete": ["DEL-123"]
                }
                """;
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<OfferDto> results = new ArrayList<>();

        OfferJSONModel validModel = mock(OfferJSONModel.class);
        when(validModel.isValid()).thenReturn(true);

        // El doReturn evita el problema de la excepción Unhandled al configurar el mock
        doReturn(validModel).when(objectMapper).treeToValue(any(JsonNode.class), eq(OfferJSONModel.class));
        when(offerParserMapper.toOfferDto(validModel)).thenReturn(new OfferDto());

        offerJSONProcessor.parseAndProcess(is, results::add, deleteCollector);

        assertThat(results).hasSize(1);
        verify(deleteCollector).addId("DEL-123");
    }

    @Test
    @DisplayName("Debe capturar referencias incluso si falla la validación del modelo")
    void processOffersArray_ValidationError_CapturesRefs() throws Exception {
        String json = """
                {
                  "offers": [{
                    "ownerReference": "REF_ERROR",
                    "dealerReference": "D_ERROR"
                  }]
                }
                """;

        OfferJSONModel invalidModel = mock(OfferJSONModel.class);
        when(invalidModel.isValid()).thenReturn(false);
        doReturn(invalidModel).when(objectMapper).treeToValue(any(JsonNode.class), eq(OfferJSONModel.class));

        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        offerJSONProcessor.parseAndProcess(is, offer -> {}, deleteCollector);

        ArgumentCaptor<LCIngestionParserException> captor = ArgumentCaptor.forClass(LCIngestionParserException.class);
        verify(offerReader).addErrorToQueue(captor.capture());

        LCIngestionParserException ex = captor.getValue();
        assertThat(ex.getFailedIdentifier().getOwnerReference()).isEqualTo("REF_ERROR");
    }

    @Test
    @DisplayName("Debe lanzar LCIngestionException si el JSON es estructuralmente inválido")
    void parseAndProcess_CriticalError() {
        String badJson = "{ \"offers\": [ { \"invalid\" } ] ";
        InputStream is = new ByteArrayInputStream(badJson.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> offerJSONProcessor.parseAndProcess(is, offer -> {}, deleteCollector))
                .isInstanceOf(LCIngestionException.class);
    }

    @Test
    @DisplayName("Debe ignorar campos desconocidos usando skipChildren")
    void parseAndProcess_UnknownFields() {
        String json = """
                {
                  "metadata": { "version": 1 },
                  "offersToDelete": ["ID-X"]
                }
                """;
        InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        offerJSONProcessor.parseAndProcess(is, offer -> {}, deleteCollector);

        verify(deleteCollector).addId("ID-X");
    }

    @Test
    @DisplayName("Debe salir silenciosamente si el JSON no empieza con un objeto")
    void parseAndProcess_NotAnObject() {
        InputStream is = new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8));
        offerJSONProcessor.parseAndProcess(is, offer -> {}, deleteCollector);
        verifyNoInteractions(offerParserMapper, deleteCollector);
    }
}
