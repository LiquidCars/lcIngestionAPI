package net.liquidcars.ingestion.application.service.parser;

import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferXmlProcessorTest {

    @InjectMocks
    private OfferXmlProcessor processor;

    @Mock
    private OfferParserMapper offerParserMapper;

    @Mock
    private Consumer<OfferDto> offerConsumer;

    @Mock
    private OfferStreamItemReader offerReader;

    @Captor
    private ArgumentCaptor<OfferDto> offerCaptor;

    @Test
    void supports_ShouldReturnTrueOnlyForXml() {
        assertTrue(processor.supports("xml"));
        assertTrue(processor.supports("XML"));
        assertFalse(processor.supports("json"));
        assertFalse(processor.supports(null));
    }

    @Test
    void parseAndProcess_ShouldProcessValidXml() {
        String xml = """
            <inventory>
                <vehicle>
                  <externalId>MF-882931</externalId>
                  <vehicleType>CAR</vehicleType>
                  <brand>BMW</brand>
                  <model>3 Series</model>
                  <year>2023</year>
                  <price>34500.5</price>
                  <status>ACTIVE</status>
                  <createdAt>2026-01-27T10:00:00+01:00</createdAt>
                  <updatedAt>2026-01-27T12:00:00+01:00</updatedAt>
                  <source>motorflash</source>
              </vehicle>
            </inventory>
            """;

        when(offerParserMapper.toOfferDto(any(OfferXMLModel.class))).thenReturn(new OfferDto());

        List<OfferDto> results = new ArrayList<>();
        processor.parseAndProcess(new ByteArrayInputStream(xml.getBytes()), results::add);

        // Debe haber procesado 1 vehículo
        assertEquals(1, results.size());
        verify(offerParserMapper).toOfferDto(any(OfferXMLModel.class));
    }

    @Test
    void parseAndProcess_ShouldThrowException_OnMalformedXml() {
        InputStream invalidIs = new ByteArrayInputStream("<vehicle><externalId>123".getBytes());

        LCIngestionException exception = assertThrows(LCIngestionException.class, () ->
                processor.parseAndProcess(invalidIs, dto -> fail("No debe procesar ofertas inválidas"))
        );

        assertTrue(exception.getMessage().contains("Error during XML stream parsing"));
    }

    @Test
    void parseAndProcess_ShouldProcessMultipleValidOffers() {
        String xml = """
            <inventory>
                <vehicle>
                  <externalId>MF-882931</externalId>
                  <vehicleType>CAR</vehicleType>
                  <brand>BMW</brand>
                  <model>3 Series</model>
                  <year>2023</year>
                  <price>34500.5</price>
                  <status>ACTIVE</status>
                  <createdAt>2026-01-27T10:00:00+01:00</createdAt>
                  <updatedAt>2026-01-27T12:00:00+01:00</updatedAt>
                  <source>motorflash</source>
                </vehicle>
                <vehicle>
                  <externalId>MF-882932</externalId>
                  <vehicleType>CAR</vehicleType>
                  <brand>Audi</brand>
                  <model>A4</model>
                  <year>2022</year>
                  <price>32000.0</price>
                  <status>ACTIVE</status>
                  <createdAt>2026-01-27T11:00:00+01:00</createdAt>
                  <updatedAt>2026-01-27T13:00:00+01:00</updatedAt>
                  <source>motorflash</source>
                </vehicle>
            </inventory>
            """;

        when(offerParserMapper.toOfferDto(any(OfferXMLModel.class))).thenAnswer(invocation -> new OfferDto());

        List<OfferDto> results = new ArrayList<>();
        processor.parseAndProcess(new ByteArrayInputStream(xml.getBytes()), results::add);

        assertEquals(2, results.size());
        verify(offerParserMapper, times(2)).toOfferDto(any(OfferXMLModel.class));
    }
}
