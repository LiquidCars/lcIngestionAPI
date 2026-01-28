package net.liquidcars.ingestion.application.service.parser;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class OfferXmlProcessor implements IOfferParserService {

    @Override
    public boolean supports(String format) {
        return "xml".equalsIgnoreCase(format);
    }

    @Override
    public void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            OfferDto currentOffer = null;

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        String tagName = reader.getLocalName();

                        // En el XML de MotorFlash, cada oferta empieza con <anuncio>
                        if ("anuncio".equals(tagName)) {
                            currentOffer = new OfferDto();
                        }
                        // Si ya tenemos un objeto creado, rellenamos sus datos
                        else if (currentOffer != null) {
                            fillOfferData(tagName, reader, currentOffer);
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        // Cuando se cierra </anuncio>, enviamos la oferta al Consumer
                        if ("anuncio".equals(reader.getLocalName()) && currentOffer != null) {
                            action.accept(currentOffer);
                            currentOffer = null; // Reiniciamos para el siguiente anuncio
                        }
                        break;
                }
            }
            reader.close();
        } catch (Exception e) {
            throw new RuntimeException("Error procesando XML de MotorFlash", e);
        }
    }


    private void fillOfferData(String tagName, XMLStreamReader reader, OfferDto offer) throws Exception {
        String content = "";

        switch (tagName) {
            case "motorflashid":
                offer.setExternalId(reader.getElementText());
                break;
            case "marca":
                offer.setBrand(reader.getElementText());
                break;
            case "modelo":
                offer.setModel(reader.getElementText());
                break;
            case "precio":
                String priceStr = reader.getElementText();
                if (!priceStr.isEmpty()) {
                    offer.setPrice(new BigDecimal(priceStr));
                }
                break;
            case "fechamatriculacion":
                String dateStr = reader.getElementText();
                if (dateStr.length() >= 4) {
                    offer.setYear(Integer.parseInt(dateStr.substring(dateStr.length() - 4).trim()));
                }
                break;
        }
    }
}

