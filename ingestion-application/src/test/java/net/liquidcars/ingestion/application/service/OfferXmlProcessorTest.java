package net.liquidcars.ingestion.application.service;

import net.liquidcars.ingestion.domain.model.OfferDto;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class OfferXmlProcessorTest {

    private final OfferXmlProcessor processor = new OfferXmlProcessor();

    @Test
    void testParseAndProcessMultipleOffers() {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("motorflash-test.xml");

        assertNotNull(inputStream, "No se encontró el archivo motorflash-test.xml en resources");

        List<OfferDto> results = new ArrayList<>();

        processor.parseAndProcess(inputStream, results::add);

        // 1. Verificar cantidad total
        assertEquals(4, results.size(), "Debería haber procesado exactamente 4 anuncios");

        // 2. Verificar el 1 (BMW)
        assertEquals("77339314", results.get(0).getExternalId());
        assertEquals("BMW", results.get(0).getBrand());
        assertEquals("Serie 1", results.get(0).getModel());
        assertEquals(new BigDecimal("9425"), results.get(0).getPrice());
        assertEquals(2015, results.get(0).getYear());

        //3. Verificar el 2 (Audi)
        assertEquals("80049614", results.get(1).getExternalId());
        assertEquals("Audi", results.get(1).getBrand());
        assertEquals("Q3", results.get(1).getModel());
        assertEquals(new BigDecimal("14490.50"), results.get(1).getPrice());
        assertEquals(2014, results.get(1).getYear());

        //4. Verificar el 3 (MG)
        assertEquals("82604882", results.get(2).getExternalId());
        assertEquals("MG", results.get(2).getBrand());
        assertEquals("MG3", results.get(2).getModel());
        assertEquals(new BigDecimal("19915.00"), results.get(2).getPrice());
        assertEquals(2025, results.get(2).getYear());

        // 3. Verificar el 4 (Dacia)
        assertEquals("80931932", results.get(3).getExternalId());
        assertEquals("Dacia", results.get(3).getBrand());
        assertEquals("Sandero", results.get(3).getModel());
        assertEquals(new BigDecimal("9945.99"), results.get(3).getPrice());
        assertEquals(2017, results.get(3).getYear());

        results.forEach(offer ->
                System.out.println("Procesado: " + offer.getBrand() + " ID: " + offer.getExternalId())
        );

    }
}
