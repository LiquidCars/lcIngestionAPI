package net.liquidcars.ingestion.application.service.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class OfferJSONProcessorTest {

    private OfferJSONProcessor processor;

    @Mock
    private OfferParserMapper offerParserMapper;

    @Mock
    private OfferStreamItemReader offerReader;

    @Mock
    private JobDeleteExternalIdsCollector deleteExternalIdsCollector;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        processor = new OfferJSONProcessor(objectMapper, offerParserMapper, offerReader);
    }

    // TODO
    /*
    @Test
    void testParseAndProcessMultipleJsonOffers() throws IOException {
        Path path = Paths.get("..", "testFiles", "offers.json");
        assertTrue(Files.exists(path), "File not found at: " + path.toAbsolutePath());

        when(offerParserMapper.toOfferDto(any(OfferJSONModel.class))).thenAnswer(invocation -> {
            OfferJSONModel model = invocation.getArgument(0);
            return OfferDtoFactory.getOfferDto();
        });

        List<OfferDto> results = new ArrayList<>();

        try (InputStream inputStream = Files.newInputStream(path)) {
            processor.parseAndProcess(inputStream, results::add);
        }

        assertEquals(10, results.size(), "Should have processed 10 advertisements from the JSON");

        // 1. BMW
        assertEquals("MF-882931", results.get(0).getExternalId());
        assertEquals("BMW", results.get(0).getBrand());
        assertEquals("3 Series", results.get(0).getModel());
        assertEquals(new BigDecimal("34500.5"), results.get(0).getPrice());
        assertEquals(2023, results.get(0).getYear());
        assertEquals(OfferDto.VehicleTypeDto.CAR, results.get(0).getVehicleType());
        assertEquals(OfferDto.OfferStatusDto.ACTIVE, results.get(0).getStatus());
        assertEquals(OffsetDateTime.parse("2026-01-27T10:00:00+01:00"), results.get(0).getCreatedAt());
        assertEquals("motorflash", results.get(0).getSource());

        // 2. Audi
        assertEquals("MF-882932", results.get(1).getExternalId());
        assertEquals("Audi", results.get(1).getBrand());
        assertEquals("A4", results.get(1).getModel());
        assertEquals(new BigDecimal("32000.0"), results.get(1).getPrice());
        assertEquals(2022, results.get(1).getYear());
        assertEquals(OffsetDateTime.parse("2026-01-27T11:00:00+01:00"), results.get(1).getCreatedAt());

        // 3. Mercedes
        assertEquals("MF-882933", results.get(2).getExternalId());
        assertEquals("Mercedes", results.get(2).getBrand());
        assertEquals("C-Class", results.get(2).getModel());
        assertEquals(new BigDecimal("37000.75"), results.get(2).getPrice());

        // 4. Volkswagen
        assertEquals("MF-882934", results.get(3).getExternalId());
        assertEquals("Volkswagen", results.get(3).getBrand());
        assertEquals(2021, results.get(3).getYear());

        // 5. Toyota
        assertEquals("MF-882935", results.get(4).getExternalId());
        assertEquals("Toyota", results.get(4).getBrand());
        assertEquals(new BigDecimal("19500.0"), results.get(4).getPrice());

        // 6. Honda
        assertEquals("MF-882936", results.get(5).getExternalId());
        assertEquals("Honda", results.get(5).getBrand());
        assertEquals(new BigDecimal("22000.5"), results.get(5).getPrice());

        // 7. Ford
        assertEquals("MF-882937", results.get(6).getExternalId());
        assertEquals("Ford", results.get(6).getBrand());
        assertEquals("Focus", results.get(6).getModel());

        // 8. Nissan
        assertEquals("MF-882938", results.get(7).getExternalId());
        assertEquals("Nissan", results.get(7).getBrand());
        assertEquals(new BigDecimal("19500.75"), results.get(7).getPrice());

        // 9. Hyundai
        assertEquals("MF-882939", results.get(8).getExternalId());
        assertEquals("Hyundai", results.get(8).getBrand());
        assertEquals(2023, results.get(8).getYear());

        // 10. Kia
        assertEquals("MF-882940", results.get(9).getExternalId());
        assertEquals("Kia", results.get(9).getBrand());
        assertEquals(new BigDecimal("20000.0"), results.get(9).getPrice());
    }*/

}
