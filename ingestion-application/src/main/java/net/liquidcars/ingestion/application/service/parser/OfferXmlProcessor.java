package net.liquidcars.ingestion.application.service.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.XML.*;
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
import java.util.ArrayList;
import java.util.List;
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
        final String DEFAULT_CURRENCY = "EUR";

        switch (tagName) {
            case "motorflashid" -> model.setDealerReference(reader.getElementText());
            case "dealerid" -> model.setChannelReference(reader.getElementText());
            case "instalacion" -> model.setInstallation(reader.getElementText());
            case "mail" -> model.setMail(reader.getElementText());
            case "matricula" -> {
                String plate = reader.getElementText(); // Leemos UNA SOLA VEZ
                model.getVehicleInstance().setPlate(plate);
                model.setOwnerReference(plate);
            }
            case "chasis" -> model.getVehicleInstance().setChassisNumber(reader.getElementText());
            case "marca" -> model.getVehicleInstance().getVehicleModel().setBrand(reader.getElementText());
            case "modelo" -> model.getVehicleInstance().getVehicleModel().setModel(reader.getElementText());
            case "version" -> model.getVehicleInstance().getVehicleModel().setVersion(reader.getElementText());
            case "carroceria" -> model.getVehicleInstance().getVehicleModel().getBodyType().setKey(reader.getElementText());
            case "estado" -> model.getVehicleInstance().getState().setKey(reader.getElementText());
            //case "disponible" -> ;// TODO DONDE GUARDAR
            case "precio" -> {
                model.getPrice().setAmount(new BigDecimal(reader.getElementText()));
                model.getPrice().setCurrency(DEFAULT_CURRENCY);
            }
            case "precio_nuevo" -> {
                model.getPriceNew().setAmount(new BigDecimal(reader.getElementText()));
                model.getPrice().setCurrency(DEFAULT_CURRENCY);
            }
            case "precio_profesional" -> {
                String val = reader.getElementText();
                if (val != null && !val.isBlank()) {
                    BigDecimal amount = new BigDecimal(val.trim().replace(",", "."));
                    model.getProfessionalPrice().setAmount(amount);
                    model.getPrice().setCurrency(DEFAULT_CURRENCY);
                }
            }
            case "precio_ofertafinanciacion" -> {
                model.getFinancedPrice().setAmount(new BigDecimal(reader.getElementText()));
                model.getPrice().setCurrency(DEFAULT_CURRENCY);
            }
            case "puertas" -> model.getVehicleInstance().getVehicleModel().setNumDoors(Integer.parseInt(reader.getElementText()));
            case "cambio" -> model.getVehicleInstance().getVehicleModel().getChangeType().setKey(reader.getElementText());
            case "ancho" -> model.getVehicleInstance().getVehicleModel().setCmWidth(Integer.parseInt(reader.getElementText()));
            case "largo" -> model.getVehicleInstance().getVehicleModel().setCmLength(Integer.parseInt(reader.getElementText()));
            case "alto" -> model.getVehicleInstance().getVehicleModel().setCmHeight(Integer.parseInt(reader.getElementText()));
            case "maletero" -> model.getVehicleInstance().getVehicleModel().setLitresTrunk(Integer.parseInt(reader.getElementText()));
            case "deposito" -> model.getVehicleInstance().getVehicleModel().setLitresTank(Integer.parseInt(reader.getElementText()));
            case "aceleracion" -> model.getVehicleInstance().getVehicleModel().setAcceleration(Double.parseDouble(reader.getElementText()));
            case "velocidadmax" -> model.getVehicleInstance().getVehicleModel().setMaxSpeed(Integer.parseInt(reader.getElementText()));
            case "peso" -> model.getVehicleInstance().getVehicleModel().setKgWeight(Integer.parseInt(reader.getElementText()));
            case "marchas" -> model.getVehicleInstance().getVehicleModel().setNumGears(Integer.parseInt(reader.getElementText()));
            case "kilometros" -> model.getVehicleInstance().setMileage(Integer.parseInt(reader.getElementText()));
            case "combustible" -> model.getVehicleInstance().getVehicleModel().getFuelType().setKey(reader.getElementText());
            case "potencia" -> {
                int cvValue = (int) Double.parseDouble(reader.getElementText().trim());
                model.getVehicleInstance().getVehicleModel().setCv(cvValue);
            }
            case "garantia" -> {
                model.setGuarantee(true);
                model.setGuaranteeMonths(Integer.parseInt(reader.getElementText()));
            }
            case "iva_deducible" -> model.setTaxDeductible(Boolean.parseBoolean(reader.getElementText()));
            case "plazas" -> model.getVehicleInstance().getVehicleModel().setNumSeats(Integer.parseInt(reader.getElementText()));
            case "traccion" -> model.getVehicleInstance().getVehicleModel().getDrivetrainType().setKey(reader.getElementText());
            case "fechamatriculacion" -> {
                String dateStr = reader.getElementText();
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd / MM / yyyy");
                    java.time.LocalDate date = java.time.LocalDate.parse(dateStr.trim(), formatter);

                    model.getVehicleInstance().setRegistrationMonth(date.getMonthValue());
                    model.getVehicleInstance().setRegistrationYear(date.getYear());
                } catch (Exception e) {
                    log.warn("Formato de fecha de matriculación inválido: {}", dateStr);
                }
            }
            case "fotos" -> model.setResources(parsePhotos(reader));
            case "color" -> model.getVehicleInstance().getColor().setKey(reader.getElementText());
            // case "colortapiceria" -> // TODO DONDE GUARDAR
            case "cilindrada" -> model.getVehicleInstance().getVehicleModel().setDisplacement(Integer.parseInt(reader.getElementText()));
            case "emisiones" -> model.getVehicleInstance().getVehicleModel().setMaxEmissions(Integer.parseInt(reader.getElementText()));
            case "distintivo" -> model.getVehicleInstance().getVehicleModel().getEnvironmentalBadge().setKey(reader.getElementText());
            case "consumo" -> parseConsumos(reader, model.getVehicleInstance().getVehicleModel());
            case "pinturas" -> parseSoloMetalizado(reader, model.getVehicleInstance());
            // case "tapicerias" -> // TODO DONDE GUARDAR
            case "extras" -> parseExtras(reader, model.getVehicleInstance());
            case "serie" -> parseSerie(reader, model.getVehicleInstance());
            case "observaciones" -> model.setObs(reader.getElementText());
            case "notainterna" -> model.setInternalNotes(reader.getElementText());
        }
    }

    private void parseSerie(XMLStreamReader reader, VehicleInstanceXMLModel instance) throws Exception {
        if (instance.getEquipments() == null) instance.setEquipments(new ArrayList<>());
        String currentType = "Other";
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                switch (tagName) {
                    case "exterior" -> currentType = "Outside";
                    case "interior" -> currentType = "Inside";
                    case "confort"  -> currentType = "Comfort";
                    case "seguridad" -> currentType = "Security";
                }
                if ("equipo".equals(tagName)) {
                    CarInstanceEquipmentXMLModel equipment = new CarInstanceEquipmentXMLModel();
                    equipment.setDescription(reader.getElementText());
                    KeyValueXMLModel type = new KeyValueXMLModel();
                    type.setKey(currentType);
                    equipment.setType(type);
                    KeyValueXMLModel category = new KeyValueXMLModel();
                    category.setKey("Serial");
                    equipment.setCategory(category);
                    instance.getEquipments().add(equipment);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "serie".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private void parseExtras(XMLStreamReader reader, VehicleInstanceXMLModel instance) throws Exception {
        if (instance.getEquipments() == null) instance.setEquipments(new ArrayList<>());

        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "extra".equals(reader.getLocalName())) {
                CarInstanceEquipmentXMLModel equipment = new CarInstanceEquipmentXMLModel();
                equipment.setCode(reader.getAttributeValue(null, "cod"));
                String precioStr = reader.getAttributeValue(null, "precio");
                if (precioStr != null) {
                    BigDecimal monto = new BigDecimal(precioStr.replace(".", "").replace(",", "."));
                    equipment.setPrice(new MoneyXMLModel(monto, "EUR"));
                }
                equipment.setDescription(reader.getElementText());
                KeyValueXMLModel cat = new KeyValueXMLModel();
                cat.setKey("Extra");
                equipment.setCategory(cat);
                instance.getEquipments().add(equipment);
            } else if (event == XMLStreamConstants.END_ELEMENT && "extras".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private List<CarOfferResourceXMLModel> parsePhotos(XMLStreamReader reader) throws Exception {
        List<CarOfferResourceXMLModel> resources = new ArrayList<>();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "foto".equals(reader.getLocalName())) {
                String url = reader.getElementText();
                if (url != null && !url.isBlank()) {
                    KeyValueXMLModel<String, String> typeKV = new KeyValueXMLModel<>();
                    typeKV.setKey("UrlImage");
                    typeKV.setValue("Image by URL");
                    CarOfferResourceXMLModel resourceModel = CarOfferResourceXMLModel.builder()
                            .type(typeKV)
                            .resource(url.trim())
                            .build();

                    resources.add(resourceModel);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "fotos".equals(reader.getLocalName())) {
                break;
            }
        }
        return resources;
    }

    private void parseConsumos(XMLStreamReader reader, VehicleModelXMLModel model) throws Exception {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String subTagName = reader.getLocalName();
                String content = reader.getElementText();
                if (content != null && !content.isBlank()) {
                    switch (subTagName) {
                        case "carretera" -> model.setRoadConsumption(Double.parseDouble(content));
                        case "urbano"    -> model.setUrbanConsumption(Double.parseDouble(content));
                        case "mixto"     -> model.setAvgConsumption(Double.parseDouble(content));
                    }
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "consumo".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private void parseSoloMetalizado(XMLStreamReader reader, VehicleInstanceXMLModel instance) throws Exception {
        while (reader.hasNext()) {
            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT && "pintura".equals(reader.getLocalName())) {
                String content = reader.getElementText();

                // Si el texto contiene "metalizado", activamos el flag
                if (content != null && content.toLowerCase().contains("metalizado")) {
                    instance.setMetallicPaint(true);
                }
            }
            else if (event == XMLStreamConstants.END_ELEMENT && "pinturas".equals(reader.getLocalName())) {
                // Salimos del bucle al cerrar la etiqueta padre
                break;
            }
        }
    }
}

