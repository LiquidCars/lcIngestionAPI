package net.liquidcars.ingestion.application.service.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.XML.*;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferXmlProcessor implements IOfferParserService {

    private final OfferParserMapper offerParserMapper;
    private final OfferStreamItemReader offerReader;

    @Override
    public boolean supports(IngestionFormat format) {
        return IngestionFormat.xml.equals(format);
    }

    @Override
    public void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action, JobDeleteExternalIdsCollector deleteExternalIdsCollector) {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // Configuración para evitar ataques XXE y mejorar rendimiento
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        try {
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

            while (reader.hasNext()) {
                int event = reader.next();

                if (event == XMLStreamConstants.START_ELEMENT) {
                    String localName = reader.getLocalName();

                    if ("anuncio".equals(localName)) {
                        processAnuncio(reader, action);
                    } else if ("offersToDelete".equals(localName)) {
                        processDeletes(reader, deleteExternalIdsCollector);
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

    private void processAnuncio(XMLStreamReader reader, Consumer<OfferDto> action) {
        OfferXMLModel xmlModel = new OfferXMLModel();
        try {
            fillModelFromXml(reader, xmlModel);

            if (xmlModel.isValid()) {
                action.accept(offerParserMapper.toOfferDto(xmlModel));
            }
        } catch (Exception e) {
            ExternalIdInfoXMLModel failedId = xmlModel.getExternalIdInfo();
            ExternalIdInfoDto failedIdDto = offerParserMapper.toExternalIdInfoDto(failedId);

            log.warn("XML Record failed parsing: {}", e.getMessage());

            offerReader.addErrorToQueue(new LCIngestionParserException(
                    LCTechCauseEnum.CONVERSION_ERROR,
                    "XML item error: " + e.getMessage(),
                    e,
                    failedIdDto
            ));
        }
    }

    private void processDeletes(XMLStreamReader reader, JobDeleteExternalIdsCollector deleteExternalIdsCollector) throws Exception {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT && "id".equals(reader.getLocalName())) {
                String idToDelete = reader.getElementText();
                if (idToDelete != null && !idToDelete.isBlank()) {
                    deleteExternalIdsCollector.addId(idToDelete);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT && "offersToDelete".equals(reader.getLocalName())) {
                break;
            }
        }
    }

    private void fillModelFromXml(XMLStreamReader reader, OfferXMLModel model) throws Exception {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String tagName = reader.getLocalName();
                fillOfferData(tagName, reader, model);
            } else if (event == XMLStreamConstants.END_ELEMENT && "anuncio".equals(reader.getLocalName())) {
                return;
            }
        }
    }

    private void fillOfferData(String tagName, XMLStreamReader reader, OfferXMLModel model) throws Exception {
        final String DEFAULT_CURRENCY = "EUR";
        // privateownerregistereduserid
        // financedInstallmentAprox
        // fianancedtext
        // guaranteetext
        // pickupaddress
        // lastupdated
        // jsoncarofferid
        // participantid
        //jobidentifier
        // batchstatus
        try{
            switch (tagName) {
                case "motorflashid" -> {
                    if(model.getExternalIdInfo()==null){
                        model.setExternalIdInfo(new ExternalIdInfoXMLModel());
                    }
                    model.getExternalIdInfo().setDealerReference(reader.getElementText());
                }
                case "dealerid" -> {
                    if(model.getExternalIdInfo()==null){
                        model.setExternalIdInfo(new ExternalIdInfoXMLModel());
                    }
                    model.getExternalIdInfo().setChannelReference(reader.getElementText());
                }
                case "instalacion" -> model.setInstallation(reader.getElementText());
                case "mail" -> model.setMail(reader.getElementText());
                case "matricula" -> {
                    String plate = reader.getElementText(); // Leemos UNA SOLA VEZ
                    model.getVehicleInstance().setPlate(plate);
                    if(model.getExternalIdInfo()==null){
                        model.setExternalIdInfo(new ExternalIdInfoXMLModel());
                    }
                    model.getExternalIdInfo().setOwnerReference(plate);
                }
                case "chasis" -> model.getVehicleInstance().setChassisNumber(reader.getElementText());
                case "marca" -> model.getVehicleInstance().getVehicleModel().setBrand(reader.getElementText());
                case "modelo" -> model.getVehicleInstance().getVehicleModel().setModel(reader.getElementText());
                case "version" -> model.getVehicleInstance().getVehicleModel().setVersion(reader.getElementText());
                case "carroceria" -> {
                    String xmlValue = reader.getElementText();
                    String bodyTypeId = mapBodyTypeToId(xmlValue);
                    model.getVehicleInstance().getVehicleModel().getBodyType().setKey(bodyTypeId);
                }
                case "estado" -> {
                    String xmlValue = reader.getElementText();
                    model.getVehicleInstance().getState().setKey(mapStateToId(xmlValue));
                }
                //case "disponible" -> ;// TODO DONDE GUARDAR
                case "precio" -> {
                    model.getPrice().setAmount(new BigDecimal(reader.getElementText()));
                    model.getPrice().setCurrency(DEFAULT_CURRENCY);
                }
                case "precio_nuevo" -> {
                    model.getPriceNew().setAmount(new BigDecimal(reader.getElementText()));
                    model.getPriceNew().setCurrency(DEFAULT_CURRENCY);
                }
                case "precio_profesional" -> {
                    String val = reader.getElementText();
                    if (val != null && !val.isBlank()) {
                        BigDecimal amount = new BigDecimal(val.trim().replace(",", "."));
                        model.getProfessionalPrice().setAmount(amount);
                        model.getProfessionalPrice().setCurrency(DEFAULT_CURRENCY);
                    }
                }
                case "precio_ofertafinanciacion" -> {
                    model.getFinancedPrice().setAmount(new BigDecimal(reader.getElementText()));
                    model.getFinancedPrice().setCurrency(DEFAULT_CURRENCY);
                }
                case "puertas" -> model.getVehicleInstance().getVehicleModel().setNumDoors(Integer.parseInt(reader.getElementText()));
                case "cambio" -> {
                    String xmlValue = reader.getElementText();
                    String changeTypeId = mapChangeTypeToId(xmlValue);
                    model.getVehicleInstance().getVehicleModel().getChangeType().setKey(changeTypeId);
                }
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
                case "combustible" -> {
                    String xmlValue = reader.getElementText();
                    String fuelTypeId = mapFuelTypeToId(xmlValue);
                    model.getVehicleInstance().getVehicleModel().getFuelType().setKey(fuelTypeId);
                }
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
                case "traccion" -> {
                    String xmlValue = reader.getElementText();
                    String driveId = mapDrivetrainToId(xmlValue);
                    model.getVehicleInstance().getVehicleModel().getDrivetrainType().setKey(driveId);
                }
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
                case "color" -> {
                    String xmlColor = reader.getElementText();
                    String colorId = mapColorToId(xmlColor);
                    model.getVehicleInstance().getColor().setKey(colorId);
                }
                // case "colortapiceria" -> // TODO DONDE GUARDAR
                case "cilindrada" -> model.getVehicleInstance().getVehicleModel().setDisplacement(Integer.parseInt(reader.getElementText()));
                case "emisiones" -> model.getVehicleInstance().getVehicleModel().setMaxEmissions(Integer.parseInt(reader.getElementText()));
                case "distintivo" -> {
                    String xmlValue = reader.getElementText();
                    String badgeId = mapEnvironmentalBadgeToId(xmlValue);
                    model.getVehicleInstance().getVehicleModel().getEnvironmentalBadge().setKey(badgeId);
                }
                case "consumo" -> parseConsumos(reader, model.getVehicleInstance().getVehicleModel());
                case "pinturas" -> parseSoloMetalizado(reader, model.getVehicleInstance());
                // case "tapicerias" -> // TODO DONDE GUARDAR
                case "extras" -> parseExtras(reader, model.getVehicleInstance());
                case "serie" -> parseSerie(reader, model.getVehicleInstance());
                case "observaciones" -> model.setObs(reader.getElementText());
                case "notainterna" -> model.setInternalNotes(reader.getElementText());
            }
        }catch (Exception e){
            log.info("Error parsing XML: {}", e.getMessage());
        }
    }

    private String mapEnvironmentalBadgeToId(String xmlBadge) {
        if (xmlBadge == null || xmlBadge.isBlank()) return "None";
        String badge = xmlBadge.toUpperCase().trim();
        if (badge.contains("ECO")) return "ECO";
        if (badge.contains("C")) return "C";
        if (badge.contains("B")) return "B";
        if (badge.contains("0") || badge.contains("CERO")) return "0";

        return "None";
    }

    private String mapDrivetrainToId(String xmlDrivetrain) {
        if (xmlDrivetrain == null || xmlDrivetrain.isBlank()) return "?";
        String drive = xmlDrivetrain.toLowerCase().trim();
        return switch (drive) {
            case "delantero", "delantera", "fwd", "front" -> "FWD";
            case "trasero", "trasera", "rwd", "rear" -> "RWD";
            case "total", "awd", "4x4", "4wd", "integral" -> {
                if (drive.contains("4x4")) yield "4X4";
                yield "AWD";
            }
            default -> "?";
        };
    }

    private String mapFuelTypeToId(String xmlFuel) {
        if (xmlFuel == null || xmlFuel.isBlank()) return "?";
        String fuel = xmlFuel.toLowerCase().trim();
        if (fuel.contains("hibrido") || fuel.contains("híbrido")) {
            if (fuel.contains("diesel") || fuel.contains("diésel")) return "HD";
            if (fuel.contains("gasolina")) return "HG";
            return "H";
        }
        return switch (fuel) {
            case "gasolina", "gasoline", "benzin" -> "G";
            case "diesel", "diésel", "gasoil" -> "D";
            case "electrico", "eléctrico", "electric", "bev" -> "E";
            case "glp", "lpg" -> "L";
            case "gnc", "cng" -> "N";
            case "etanol", "bioetanol" -> "M";
            default -> "?";
        };
    }

    private String mapChangeTypeToId(String xmlChange) {
        if (xmlChange == null || xmlChange.isBlank()) return "?";
        String change = xmlChange.toLowerCase().trim();
        if (change.contains("manual") || change.equals("m")) {
            return "M";
        } else if (change.contains("auto") || change.contains("secuencial") || change.equals("a")) {
            return "A";
        }
        return "?";
    }

    private String mapBodyTypeToId(String xmlBodyType) {
        if (xmlBodyType == null || xmlBodyType.isBlank()) return "?";
        String body = xmlBodyType.toLowerCase().trim();
        return switch (body) {
            case "compacto", "compact", "pequeño", "small", "utilitario" -> "1";
            case "berlina", "sedan", "saloon", "sedán" -> "2";
            case "familiar", "family", "ranchera", "station wagon", "avant", "touring" -> "3";
            case "suv", "todoterreno", "todo terreno", "4x4", "all-terrain", "crossover" -> "4";
            case "cabrio", "convertible", "descapotable" -> "5";
            case "deportivo", "coupe", "coupé", "sport" -> "6";
            case "monovolumen", "minivan", "mpv" -> "9";
            case "pickup", "pick-up" -> "10";
            case "furgon", "furgón", "van", "combi" -> "23";
            default -> "?";
        };
    }

    private String mapStateToId(String xmlState) {
        if (xmlState == null || xmlState.isBlank()) return "?";
        String state = xmlState.toLowerCase().trim();
        return switch (state) {
            case "nuevo", "new" -> "New";
            case "usado", "used" -> "Used";
            case "oportunidad", "opportunity", "ocasion", "ocasión" -> "Opportunity";
            case "km0", "km 0", "pre-registered", "seminuevo" -> "KM0";
            default -> "?";
        };
    }

    private String mapColorToId(String xmlColor) {
        if (xmlColor == null || xmlColor.isBlank()) return null;
        String color = xmlColor.toLowerCase().trim();
        return switch (color) {
            case "amarillo", "yellow" -> "1";
            case "azul", "blue" -> "2";
            case "azul claro", "light blue", "celeste" -> "3";
            case "beige", "beig" -> "4";
            case "blanco", "white" -> "5";
            case "bronce", "bronze" -> "6";
            case "oro", "dorado", "gold" -> "7";
            case "gris", "grey", "gray" -> "8";
            case "gris claro", "light grey", "silver grey" -> "9";
            case "marron", "marrón", "brown" -> "10";
            case "naranja", "orange" -> "11";
            case "negro", "black" -> "12";
            case "plata", "plateado", "silver" -> "13";
            case "rojo", "red" -> "14";
            case "dark red", "burdeos" -> "15";
            case "verde", "green" -> "16";
            case "verde claro", "light green" -> "17";
            case "violeta", "morado", "violet", "purple" -> "18";
            case "granate", "grenade" -> "19";
            default -> "?";
        };
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
                if (content != null && content.toLowerCase().contains("metalizado")) {
                    instance.setMetallicPaint(true);
                }
            }
            else if (event == XMLStreamConstants.END_ELEMENT && "pinturas".equals(reader.getLocalName())) {
                break;
            }
        }
    }
}

