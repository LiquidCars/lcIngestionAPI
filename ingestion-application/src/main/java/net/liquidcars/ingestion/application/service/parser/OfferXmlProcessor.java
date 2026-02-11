package net.liquidcars.ingestion.application.service.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.XML.CarOfferSellerTypeEnumXMLModel;
import net.liquidcars.ingestion.application.service.parser.model.XML.MoneyXMLModel;
import net.liquidcars.ingestion.application.service.parser.model.XML.OfferXMLModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferXmlProcessor implements IOfferParserService {

    private final OfferParserMapper offerParserMapper;
    private final OfferStreamItemReader offerReader;

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
                    OfferXMLModel xmlModel = new OfferXMLModel();
                    try {
                        fillModelFromXml(reader, xmlModel);

                        if (xmlModel.isValid()) {
                            action.accept(offerParserMapper.toOfferDto(xmlModel));
                        }
                    } catch (Exception e) {
                        UUID failedId = (xmlModel.getId() != null) ? xmlModel.getId() : null;

                        log.warn("XML Record {} failed parsing: {}", failedId, e.getMessage());

                        offerReader.addErrorToQueue(new LCIngestionParserException(
                                LCTechCauseEnum.CONVERSION_ERROR,
                                "XML item error: " + e.getMessage(),
                                e,
                                failedId
                        ));
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            log.error("Fatal error reading XML stream", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.CONVERSION_ERROR)
                    .message("Error during XML stream parsing: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }

    private void fillModelFromXml(XMLStreamReader reader, OfferXMLModel model) throws Exception {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                fillOfferData(tagName, reader, model);
            } else if (event == XMLStreamConstants.END_ELEMENT && "vehicle".equals(reader.getLocalName())) {
                return;
            }
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

        final String DEFAULT_CURRENCY = "EUR";

        switch (tagName) {
            case "id" -> model.setId(UUID.fromString(content));
            case "xmlCarOfferId" -> model.setXmlCarOfferId(UUID.fromString(content));
            case "privateOwnerRegisteredUserId" -> model.setPrivateOwnerRegisteredUserId(UUID.fromString(content));
            case "sellerType" -> model.setSellerType(CarOfferSellerTypeEnumXMLModel.valueOf(content));
            case "price" -> model.setPrice(MoneyXMLModel.toMoney(new BigDecimal(content), DEFAULT_CURRENCY));
            case "financedPrice" -> model.setFinancedPrice(MoneyXMLModel.toMoney(new BigDecimal(content), DEFAULT_CURRENCY));
            case "financedInstallmentAprox" -> model.setFinancedInstallmentAprox(MoneyXMLModel.toMoney(new BigDecimal(content), DEFAULT_CURRENCY));
            case "priceNew" -> model.setPriceNew(MoneyXMLModel.toMoney(new BigDecimal(content), DEFAULT_CURRENCY));
            case "professionalPrice" -> model.setProfessionalPrice(MoneyXMLModel.toMoney(new BigDecimal(content), DEFAULT_CURRENCY));
            case "ownerReference" -> model.setOwnerReference(content);
            case "dealerReference" -> model.setDealerReference(content);
            case "channelReference" -> model.setChannelReference(content);
            case "financedText" -> model.setFinancedText(content);
            case "obs" -> model.setObs(content);
            case "internalNotes" -> model.setInternalNotes(content);
            case "installation" -> model.setInstallation(content);
            case "mail" -> model.setMail(content);
            case "taxDeductible" -> model.setTaxDeductible(Boolean.parseBoolean(content));
            case "guarantee" -> model.setGuarantee(Boolean.parseBoolean(content));
            case "certified" -> model.setCertified(Boolean.parseBoolean(content));
            case "guaranteeMonths" -> model.setGuaranteeMonths(Integer.parseInt(content));
            case "hash" -> model.setHash(Integer.parseInt(content));
            case "lastUpdated" -> model.setLastUpdated(Long.parseLong(content));
        }
    }
}

