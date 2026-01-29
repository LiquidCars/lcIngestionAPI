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
import java.time.OffsetDateTime;
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
                        if ("vehicle".equals(tagName)) {
                            currentOffer = new OfferDto();
                        }
                        else if (currentOffer != null) {
                            fillOfferData(tagName, reader, currentOffer);
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        if ("vehicle".equals(reader.getLocalName()) && currentOffer != null) {
                            action.accept(currentOffer);
                            currentOffer = null;
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
        String content = reader.getElementText();
        if (content == null || content.isBlank()) return;

        switch (tagName) {
            case "externalId":
                offer.setExternalId(content);
                break;
            case "vehicleType":
                offer.setVehicleType(OfferDto.VehicleTypeDto.valueOf(content.toUpperCase()));
                break;
            case "brand":
                offer.setBrand(content);
                break;
            case "model":
                offer.setModel(content);
                break;
            case "year":
                offer.setYear(Integer.parseInt(content));
                break;
            case "price":
                offer.setPrice(new BigDecimal(content));
                break;
            case "status":
                offer.setStatus(OfferDto.OfferStatusDto.valueOf(content.toUpperCase()));
                break;
            case "createdAt":
                offer.setCreatedAt(OffsetDateTime.parse(content));
                break;
            case "updatedAt":
                offer.setUpdatedAt(OffsetDateTime.parse(content));
                break;
            case "source":
                offer.setSource(content);
                break;
        }
    }
}

