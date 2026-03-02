package net.liquidcars.ingestion.application.service.parser;

import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OfferXmlProcessorTest {

    @Mock
    private OfferParserMapper offerParserMapper;

    @InjectMocks
    private OfferXmlProcessor processor;

    @Mock
    private JobDeleteExternalIdsCollector deleteExternalIdsCollector;

    @Mock
    private OfferStreamItemReader offerReader;

    private static final String STARTUP_FILE = "static/testFiles/MF_856b7d9a-cabd-4e86-aaca-b7a9641a9d0b_CC_2024_09_30_A.xml";


    @Test
    void supports_ShouldReturnTrueOnlyForXml() {
        assertTrue(processor.supports(IngestionFormat.xml));
        assertFalse(processor.supports(IngestionFormat.json));
        assertFalse(processor.supports(null));
    }

    @Test
    void parseAndProcess_ShouldThrowRuntimeException_OnInvalidXml() {
        InputStream invalidIs = new ByteArrayInputStream("<vehicle><externalId>123".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                processor.parseAndProcess(invalidIs, dto -> {}, deleteExternalIdsCollector)
        );

        // Check for the actual parser error message instead of the custom one
        assertTrue(ex.getMessage().contains("Unexpected EOF") ||
                ex.getMessage().contains("expecting a close tag"));
    }

    @Test
    @DisplayName("Debe parsear correctamente un XML con un anuncio y llamar al action")
    void parseAndProcess_ShouldProcessAnuncio() {
        String xml = """
            <root>
                <anuncio>
                    <motorflashid>MF-123</motorflashid>
                    <marca>BMW</marca>
                    <modelo>Serie 3</modelo>
                    <precio>30000</precio>
                    <fotos>
                        <foto>http://image.com/1.jpg</foto>
                    </fotos>
                </anuncio>
            </root>
            """;
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        // USO DE INSTANCIO: Generamos el DTO de salida esperado con datos aleatorios
        OfferDto expectedDto = TestDataFactory.createOfferDto();

        when(offerParserMapper.toOfferDto(any(OfferXMLModel.class))).thenReturn(expectedDto);

        // WHEN
        List<OfferDto> results = new ArrayList<>();
        processor.parseAndProcess(inputStream, results::add, deleteExternalIdsCollector);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(expectedDto);
        verify(offerParserMapper).toOfferDto(argThat((OfferXMLModel model) ->
                "BMW".equals(model.getVehicleInstance().getVehicleModel().getBrand()) &&
                        model.getPrice().getAmount().compareTo(new BigDecimal("30000")) == 0
        ));
    }

    @Test
    @DisplayName("Debe recolectar IDs de ofertas a borrar")
    void parseAndProcess_ShouldCollectDeletes() {
        // GIVEN
        String xml = """
            <root>
                <offersToDelete>
                    <id>DEL-001</id>
                    <id>DEL-002</id>
                </offersToDelete>
            </root>
            """;
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());

        // WHEN
        processor.parseAndProcess(inputStream, dto -> {}, deleteExternalIdsCollector);

        // THEN
        verify(deleteExternalIdsCollector).addId("DEL-001");
        verify(deleteExternalIdsCollector).addId("DEL-002");
    }

    @Test
    void parseAndProcess_ShouldHandleDeletes() {
        String xml = """
    <root>
        <offersToDelete>
            <id>123</id>
            <id>456</id>
        </offersToDelete>
    </root>
    """;
        processor.parseAndProcess(new ByteArrayInputStream(xml.getBytes()), d -> {}, deleteExternalIdsCollector);
        verify(deleteExternalIdsCollector, times(2)).addId(anyString());
    }

    @Test
    void parseAndProcess_ShouldAddErrorToQueue_WhenMapperFails() throws Exception {
        InputStream inputStream =
                getClass().getClassLoader()
                        .getResourceAsStream(STARTUP_FILE);

        when(offerParserMapper.toOfferDto(any(OfferXMLModel.class)))
                .thenThrow(new RuntimeException("Mapping error"));

        processor.parseAndProcess(
                inputStream,
                dto -> {},
                deleteExternalIdsCollector
        );

        verify(offerReader, times(169)).addErrorToQueue(any());
    }

    @Test
    void supports_ShouldReturnTrue_ForSupportedClass() {
        boolean result = processor.supports(IngestionFormat.xml);
        assertTrue(result, "El processor debería soportar OfferXMLModel.class");
    }


}