package net.liquidcars.ingestion.application.service.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.OfferJSONModel;
import net.liquidcars.ingestion.application.service.parser.model.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferXmlProcessor implements IOfferParserService {

    private final OfferParserMapper offerParserMapper;

    @Override
    public boolean supports(String format) {
        return "xml".equalsIgnoreCase(format);
    }

    @Override
    public void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT && "vehicle".equals(reader.getLocalName())) {
                    OfferXMLModel xmlModel = buildModelFromXml(reader);
                    if (xmlModel != null && xmlModel.isValid()) {
                        action.accept(offerParserMapper.toOfferDto(xmlModel));
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            log.error("Streaming XML error: {}", e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.CONVERSION_ERROR)
                    .message("Error during XML stream parsing: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }

    private OfferXMLModel buildModelFromXml(XMLStreamReader reader) throws Exception {
        OfferXMLModel model = new OfferXMLModel();
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                fillOfferData(tagName, reader, model);
            }
            else if (event == XMLStreamConstants.END_ELEMENT && "vehicle".equals(reader.getLocalName())) {
                return model;
            }
        }
        return model;
    }

    private void fillOfferData(String tagName, XMLStreamReader reader, OfferXMLModel model) throws Exception {
        String content = reader.getElementText();
        if (content == null || content.isBlank()) return;

        switch (tagName) {
            case "externalId"  -> model.setExternalId(content);
            case "brand"       -> model.setBrand(content);
            case "model"       -> model.setModel(content);
            case "year"        -> model.setYear(Integer.parseInt(content));
            case "price"       -> model.setPrice(new BigDecimal(content));
            case "source"      -> model.setSource(content);
            case "vehicleType" -> model.setVehicleType(OfferXMLModel.VehicleTypeXML.valueOf(content.toUpperCase()));
            case "status"      -> model.setStatus(OfferXMLModel.OfferStatusXML.valueOf(content.toUpperCase()));
            case "createdAt"   -> model.setCreatedAt(OffsetDateTime.parse(content));
            case "updatedAt"   -> model.setUpdatedAt(OffsetDateTime.parse(content));
        }
    }
}

