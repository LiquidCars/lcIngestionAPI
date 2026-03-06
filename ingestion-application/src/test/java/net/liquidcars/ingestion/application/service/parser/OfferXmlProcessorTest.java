package net.liquidcars.ingestion.application.service.parser;

import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.factory.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void testMapColorToId_allCases() {

        // NULL, empty, blank
        assertNull(invoke("mapColorToId", null));
        assertNull(invoke("mapColorToId", ""));
        assertNull(invoke("mapColorToId", "   "));

        assertEquals("1", invoke("mapColorToId", "amarillo"));
        assertEquals("1", invoke("mapColorToId", "yellow"));

        assertEquals("2", invoke("mapColorToId", "azul"));
        assertEquals("2", invoke("mapColorToId", "blue"));

        assertEquals("3", invoke("mapColorToId", "azul claro"));
        assertEquals("3", invoke("mapColorToId", "light blue"));
        assertEquals("3", invoke("mapColorToId", "celeste"));

        assertEquals("4", invoke("mapColorToId", "beige"));
        assertEquals("4", invoke("mapColorToId", "beig"));

        assertEquals("5", invoke("mapColorToId", "blanco"));
        assertEquals("5", invoke("mapColorToId", "white"));

        assertEquals("6", invoke("mapColorToId", "bronce"));
        assertEquals("6", invoke("mapColorToId", "bronze"));

        assertEquals("7", invoke("mapColorToId", "oro"));
        assertEquals("7", invoke("mapColorToId", "dorado"));
        assertEquals("7", invoke("mapColorToId", "gold"));

        assertEquals("8", invoke("mapColorToId", "gris"));
        assertEquals("8", invoke("mapColorToId", "grey"));
        assertEquals("8", invoke("mapColorToId", "gray"));

        assertEquals("9", invoke("mapColorToId", "gris claro"));
        assertEquals("9", invoke("mapColorToId", "light grey"));
        assertEquals("9", invoke("mapColorToId", "silver grey"));

        assertEquals("10", invoke("mapColorToId", "marron"));
        assertEquals("10", invoke("mapColorToId", "marrón"));
        assertEquals("10", invoke("mapColorToId", "brown"));

        assertEquals("11", invoke("mapColorToId", "naranja"));
        assertEquals("11", invoke("mapColorToId", "orange"));

        assertEquals("12", invoke("mapColorToId", "negro"));
        assertEquals("12", invoke("mapColorToId", "black"));

        assertEquals("13", invoke("mapColorToId", "plata"));
        assertEquals("13", invoke("mapColorToId", "plateado"));
        assertEquals("13", invoke("mapColorToId", "silver"));

        assertEquals("14", invoke("mapColorToId", "rojo"));
        assertEquals("14", invoke("mapColorToId", "red"));

        assertEquals("15", invoke("mapColorToId", "dark red"));
        assertEquals("15", invoke("mapColorToId", "burdeos"));

        assertEquals("16", invoke("mapColorToId", "verde"));
        assertEquals("16", invoke("mapColorToId", "green"));

        assertEquals("17", invoke("mapColorToId", "verde claro"));
        assertEquals("17", invoke("mapColorToId", "light green"));

        assertEquals("18", invoke("mapColorToId", "violeta"));
        assertEquals("18", invoke("mapColorToId", "morado"));
        assertEquals("18", invoke("mapColorToId", "violet"));
        assertEquals("18", invoke("mapColorToId", "purple"));

        assertEquals("19", invoke("mapColorToId", "granate"));
        assertEquals("19", invoke("mapColorToId", "grenade"));

        assertEquals("?", invoke("mapColorToId", "color-inventado"));
    }

    @Test
    void testMapChangeTypeToId() {

        // NULL, empty, blank
        assertEquals("?", invoke("mapChangeTypeToId", null));
        assertEquals("?", invoke("mapChangeTypeToId", ""));
        assertEquals("?", invoke("mapChangeTypeToId", "   "));

        assertEquals("M", invoke("mapChangeTypeToId", "manual"));
        assertEquals("M", invoke("mapChangeTypeToId", "Manual"));
        assertEquals("M", invoke("mapChangeTypeToId", "m"));

        assertEquals("A", invoke("mapChangeTypeToId", "auto"));
        assertEquals("A", invoke("mapChangeTypeToId", "automatic"));
        assertEquals("A", invoke("mapChangeTypeToId", "secuencial"));
        assertEquals("A", invoke("mapChangeTypeToId", "a"));

        assertEquals("?", invoke("mapChangeTypeToId", "random"));
    }

    @Test
    void testMapStateToId() {
        assertEquals("?", invoke("mapStateToId", null));
        assertEquals("?", invoke("mapStateToId", ""));
        assertEquals("?", invoke("mapStateToId", "   "));

        assertEquals("New", invoke("mapStateToId", "new"));
        assertEquals("Used", invoke("mapStateToId", "used"));

        assertEquals("?", invoke("mapStateToId", "random"));
    }

    @Test
    void testMapBodyTypeToId() {
        assertEquals("?", invoke("mapBodyTypeToId", null));
        assertEquals("?", invoke("mapBodyTypeToId", ""));
        assertEquals("?", invoke("mapBodyTypeToId", "   "));

        assertEquals("1", invoke("mapBodyTypeToId", "compacto"));
        assertEquals("1", invoke("mapBodyTypeToId", "compact"));
        assertEquals("1", invoke("mapBodyTypeToId", "pequeño"));
        assertEquals("1", invoke("mapBodyTypeToId", "small"));
        assertEquals("1", invoke("mapBodyTypeToId", "utilitario"));

        assertEquals("2", invoke("mapBodyTypeToId", "berlina"));
        assertEquals("2", invoke("mapBodyTypeToId", "sedan"));
        assertEquals("2", invoke("mapBodyTypeToId", "saloon"));
        assertEquals("2", invoke("mapBodyTypeToId", "sedán"));

        assertEquals("3", invoke("mapBodyTypeToId", "familiar"));
        assertEquals("3", invoke("mapBodyTypeToId", "family"));
        assertEquals("3", invoke("mapBodyTypeToId", "ranchera"));
        assertEquals("3", invoke("mapBodyTypeToId", "station wagon"));
        assertEquals("3", invoke("mapBodyTypeToId", "avant"));
        assertEquals("3", invoke("mapBodyTypeToId", "touring"));

        assertEquals("4", invoke("mapBodyTypeToId", "suv"));
        assertEquals("4", invoke("mapBodyTypeToId", "todoterreno"));
        assertEquals("4", invoke("mapBodyTypeToId", "todo terreno"));
        assertEquals("4", invoke("mapBodyTypeToId", "4x4"));
        assertEquals("4", invoke("mapBodyTypeToId", "all-terrain"));
        assertEquals("4", invoke("mapBodyTypeToId", "crossover"));

        assertEquals("5", invoke("mapBodyTypeToId", "cabrio"));
        assertEquals("5", invoke("mapBodyTypeToId", "convertible"));
        assertEquals("5", invoke("mapBodyTypeToId", "descapotable"));

        assertEquals("6", invoke("mapBodyTypeToId", "deportivo"));
        assertEquals("6", invoke("mapBodyTypeToId", "coupe"));
        assertEquals("6", invoke("mapBodyTypeToId", "coupé"));
        assertEquals("6", invoke("mapBodyTypeToId", "sport"));

        assertEquals("9", invoke("mapBodyTypeToId", "monovolumen"));
        assertEquals("9", invoke("mapBodyTypeToId", "minivan"));
        assertEquals("9", invoke("mapBodyTypeToId", "mpv"));

        assertEquals("10", invoke("mapBodyTypeToId", "pickup"));
        assertEquals("10", invoke("mapBodyTypeToId", "pick-up"));

        assertEquals("23", invoke("mapBodyTypeToId", "furgon"));
        assertEquals("23", invoke("mapBodyTypeToId", "furgón"));
        assertEquals("23", invoke("mapBodyTypeToId", "van"));
        assertEquals("23", invoke("mapBodyTypeToId", "combi"));

        assertEquals("?", invoke("mapBodyTypeToId", "tipo-inventado"));
    }

    @Test
    void testMapFuelTypeToId() {
        assertEquals("?", invoke("mapFuelTypeToId", null));
        assertEquals("?", invoke("mapFuelTypeToId", ""));
        assertEquals("?", invoke("mapFuelTypeToId", "   "));

        assertEquals("G", invoke("mapFuelTypeToId", "gasolina"));
        assertEquals("G", invoke("mapFuelTypeToId", "gasoline"));
        assertEquals("G", invoke("mapFuelTypeToId", "benzin"));

        assertEquals("D", invoke("mapFuelTypeToId", "diesel"));
        assertEquals("D", invoke("mapFuelTypeToId", "diésel"));
        assertEquals("D", invoke("mapFuelTypeToId", "gasoil"));

        assertEquals("E", invoke("mapFuelTypeToId", "electrico"));
        assertEquals("E", invoke("mapFuelTypeToId", "eléctrico"));
        assertEquals("E", invoke("mapFuelTypeToId", "electric"));
        assertEquals("E", invoke("mapFuelTypeToId", "bev"));

        assertEquals("L", invoke("mapFuelTypeToId", "glp"));
        assertEquals("L", invoke("mapFuelTypeToId", "lpg"));

        assertEquals("N", invoke("mapFuelTypeToId", "gnc"));
        assertEquals("N", invoke("mapFuelTypeToId", "cng"));

        assertEquals("M", invoke("mapFuelTypeToId", "etanol"));
        assertEquals("M", invoke("mapFuelTypeToId", "bioetanol"));

        assertEquals("H", invoke("mapFuelTypeToId", "hibrido"));
        assertEquals("H", invoke("mapFuelTypeToId", "híbrido"));
        assertEquals("HD", invoke("mapFuelTypeToId", "hibrido diesel"));
        assertEquals("HD", invoke("mapFuelTypeToId", "híbrido diésel"));
        assertEquals("HG", invoke("mapFuelTypeToId", "hibrido gasolina"));
        assertEquals("HG", invoke("mapFuelTypeToId", "híbrido gasolina"));

        assertEquals("?", invoke("mapFuelTypeToId", "fuel-inventado"));
    }

    private String invoke(String methodName, String input) {
        return (String) ReflectionTestUtils.invokeMethod(processor, methodName, input);
    }
}