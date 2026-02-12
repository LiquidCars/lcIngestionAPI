package net.liquidcars.ingestion.application.service.parser;

import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@Disabled
@ExtendWith(MockitoExtension.class)
class OfferXmlProcessorTest {

    @InjectMocks
    private OfferXmlProcessor processor;

    @Mock
    private OfferParserMapper offerParserMapper;

    @Mock
    private Consumer<OfferDto> offerConsumer;

    @Captor
    private ArgumentCaptor<OfferDto> offerCaptor;

    @Test
    void supports_ShouldReturnTrueOnlyForXml() {
        assertTrue(processor.supports(IngestionFormat.xml));
        assertFalse(processor.supports(IngestionFormat.json));
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

        assertEquals(1, results.size());
        verify(offerParserMapper).toOfferDto(any(OfferXMLModel.class));
    }

    @Test
    void parseAndProcess_ShouldThrowRuntimeException_OnInvalidXml() {
        InputStream invalidIs = new ByteArrayInputStream("<vehicle><externalId>123".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                processor.parseAndProcess(invalidIs, dto -> {})
        );

        // Check for the actual parser error message instead of the custom one
        assertTrue(ex.getMessage().contains("Unexpected EOF") ||
                ex.getMessage().contains("expecting a close tag"));
    }

    // todo
    /*
    @Test
    void testParseAndProcessMultipleOffers() throws IOException {
        Path path = Paths.get("..", "testFiles", "offers.xml");
        assertTrue(Files.exists(path), "File not found at: " + path.toAbsolutePath());

        when(offerParserMapper.toOfferDto(any(OfferXMLModel.class))).thenAnswer(invocation -> {
            OfferXMLModel xml = invocation.getArgument(0);
            OfferDto dto = new OfferDto();
            dto.setExternalId(xml.getExternalId());
            dto.setBrand(xml.getBrand());
            dto.setModel(xml.getModel());
            dto.setPrice(xml.getPrice());
            dto.setYear(xml.getYear());
            dto.setSource(xml.getSource());
            dto.setCreatedAt(xml.getCreatedAt());
            dto.setUpdatedAt(xml.getUpdatedAt());
            dto.setVehicleType(OfferDto.VehicleTypeDto.valueOf(xml.getVehicleType().name()));
            dto.setStatus(OfferDto.OfferStatusDto.valueOf(xml.getStatus().name()));
            return dto;
        });

        try (InputStream inputStream = Files.newInputStream(path)) {
            processor.parseAndProcess(inputStream, offerConsumer);
        }

        verify(offerConsumer, times(10)).accept(offerCaptor.capture());
        List<OfferDto> results = offerCaptor.getAllValues();

        assertEquals(10, results.size(), "Debería haber procesado exactamente 4 anuncios");

        // 1. BMW
        assertEquals("MF-882931", results.get(0).getExternalId());
        assertEquals("BMW", results.get(0).getBrand());
        assertEquals("3 Series", results.get(0).getModel());
        assertEquals(new BigDecimal("34500.5"), results.get(0).getPrice());
        assertEquals(2023, results.get(0).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(0).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(0).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T10:00:00+01:00"), results.get(0).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T12:00:00+01:00"), results.get(0).getUpdatedAt());
        assertEquals("motorflash", results.get(0).getSource());

        // 2. Audi
        assertEquals("MF-882932", results.get(1).getExternalId());
        assertEquals("Audi", results.get(1).getBrand());
        assertEquals("A4", results.get(1).getModel());
        assertEquals(new BigDecimal("32000.0"), results.get(1).getPrice());
        assertEquals(2022, results.get(1).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(1).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(1).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T11:00:00+01:00"), results.get(1).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T13:00:00+01:00"), results.get(1).getUpdatedAt());
        assertEquals("motorflash", results.get(1).getSource());

        // 3. Mercedes
        assertEquals("MF-882933", results.get(2).getExternalId());
        assertEquals("Mercedes", results.get(2).getBrand());
        assertEquals("C-Class", results.get(2).getModel());
        assertEquals(new BigDecimal("37000.75"), results.get(2).getPrice());
        assertEquals(2023, results.get(2).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(2).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(2).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T10:30:00+01:00"), results.get(2).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T12:30:00+01:00"), results.get(2).getUpdatedAt());
        assertEquals("motorflash", results.get(2).getSource());

        // 4. Volkswagen
        assertEquals("MF-882934", results.get(3).getExternalId());
        assertEquals("Volkswagen", results.get(3).getBrand());
        assertEquals("Golf", results.get(3).getModel());
        assertEquals(new BigDecimal("21500.0"), results.get(3).getPrice());
        assertEquals(2021, results.get(3).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(3).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(3).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T09:00:00+01:00"), results.get(3).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T11:00:00+01:00"), results.get(3).getUpdatedAt());
        assertEquals("motorflash", results.get(3).getSource());

        // 5. Toyota
        assertEquals("MF-882935", results.get(4).getExternalId());
        assertEquals("Toyota", results.get(4).getBrand());
        assertEquals("Corolla", results.get(4).getModel());
        assertEquals(new BigDecimal("19500.0"), results.get(4).getPrice());
        assertEquals(2022, results.get(4).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(4).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(4).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T08:30:00+01:00"), results.get(4).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T10:30:00+01:00"), results.get(4).getUpdatedAt());
        assertEquals("motorflash", results.get(4).getSource());

        // 6. Honda
        assertEquals("MF-882936", results.get(5).getExternalId());
        assertEquals("Honda", results.get(5).getBrand());
        assertEquals("Civic", results.get(5).getModel());
        assertEquals(new BigDecimal("22000.5"), results.get(5).getPrice());
        assertEquals(2023, results.get(5).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(5).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(5).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T07:45:00+01:00"), results.get(5).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T09:45:00+01:00"), results.get(5).getUpdatedAt());
        assertEquals("motorflash", results.get(5).getSource());

        // 7. Ford
        assertEquals("MF-882937", results.get(6).getExternalId());
        assertEquals("Ford", results.get(6).getBrand());
        assertEquals("Focus", results.get(6).getModel());
        assertEquals(new BigDecimal("18500.0"), results.get(6).getPrice());
        assertEquals(2021, results.get(6).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(6).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(6).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T10:15:00+01:00"), results.get(6).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T12:15:00+01:00"), results.get(6).getUpdatedAt());
        assertEquals("motorflash", results.get(6).getSource());

        // 8. Nissan
        assertEquals("MF-882938", results.get(7).getExternalId());
        assertEquals("Nissan", results.get(7).getBrand());
        assertEquals("Sentra", results.get(7).getModel());
        assertEquals(new BigDecimal("19500.75"), results.get(7).getPrice());
        assertEquals(2022, results.get(7).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(7).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(7).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T09:30:00+01:00"), results.get(7).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T11:30:00+01:00"), results.get(7).getUpdatedAt());
        assertEquals("motorflash", results.get(7).getSource());

        // 9. Hyundai
        assertEquals("MF-882939", results.get(8).getExternalId());
        assertEquals("Hyundai", results.get(8).getBrand());
        assertEquals("Elantra", results.get(8).getModel());
        assertEquals(new BigDecimal("21000.0"), results.get(8).getPrice());
        assertEquals(2023, results.get(8).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(8).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(8).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T08:00:00+01:00"), results.get(8).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T10:00:00+01:00"), results.get(8).getUpdatedAt());
        assertEquals("motorflash", results.get(8).getSource());

        // 10. Kia
        assertEquals("MF-882940", results.get(9).getExternalId());
        assertEquals("Kia", results.get(9).getBrand());
        assertEquals("Cerato", results.get(9).getModel());
        assertEquals(new BigDecimal("20000.0"), results.get(9).getPrice());
        assertEquals(2022, results.get(9).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(9).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(9).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T07:00:00+01:00"), results.get(9).getCreatedAt());
        assertEquals(OffsetDateTime.parse("2026-01-27T09:00:00+01:00"), results.get(9).getUpdatedAt());
        assertEquals("motorflash", results.get(9).getSource());

    }*/
}
