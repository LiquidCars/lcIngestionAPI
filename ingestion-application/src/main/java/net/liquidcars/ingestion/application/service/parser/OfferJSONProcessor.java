package net.liquidcars.ingestion.application.service.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;
import net.liquidcars.ingestion.application.service.batch.OfferStreamItemReader;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.JSON.OfferJSONModel;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionParserException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferJSONProcessor implements IOfferParserService {

    private final ObjectMapper objectMapper;
    private final OfferParserMapper offerParserMapper;
    private final OfferStreamItemReader offerReader;

    @Override
    public boolean supports(IngestionFormat format) {
        return IngestionFormat.json.equals(format);
    }

    @Override
    public void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action, JobDeleteExternalIdsCollector deleteExternalIdsCollector) {
        try (JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) return;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.currentName();
                JsonToken token = parser.nextToken();
                if ("offers".equals(fieldName)) {
                    processOffersArray(parser, action);
                } else if ("offersToDelete".equals(fieldName)) {
                    processDeleteArray(parser, deleteExternalIdsCollector);
                } else {
                    parser.skipChildren();
                }
            }
        } catch (Exception e) {
            log.error("Critical error reading JSON stream", e);
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.CONVERSION_ERROR)
                    .message("Fatal error during JSON stream parsing: " + e.getMessage())
                    .build();
        }
    }

    private void processOffersArray(JsonParser parser, Consumer<OfferDto> action) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) return;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            ExternalIdInfoDto currentRef = new ExternalIdInfoDto();
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(parser);

                if (node != null) {
                    extractReferences(node, currentRef);

                    OfferJSONModel model = objectMapper.treeToValue(node, OfferJSONModel.class);
                    if (model != null && model.isValid()) {
                        action.accept(offerParserMapper.toOfferDto(model));
                    } else {
                        throw new IllegalArgumentException("Validation failed for the current vehicle model");
                    }
                }
            } catch (Exception e) {
                String errorRef = String.format("Owner: %s, Dealer: %s, Channel: %s",
                        currentRef.getOwnerReference(), currentRef.getDealerReference(), currentRef.getChannelReference());

                log.warn("Record [{}] failed parsing: {}", errorRef, e.getMessage());

                offerReader.addErrorToQueue(new LCIngestionParserException(
                        LCTechCauseEnum.CONVERSION_ERROR,
                        "JSON item error: " + e.getMessage(),
                        e,
                        currentRef
                ));
            }
        }
    }

    private void processDeleteArray(JsonParser parser, JobDeleteExternalIdsCollector deleteExternalIdsCollector) throws IOException {
        if (parser.currentToken() != JsonToken.START_ARRAY) return;

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            String idToDelete = parser.getText();
            if (idToDelete != null && !idToDelete.isEmpty()) {
                deleteExternalIdsCollector.addId(idToDelete);
            }
        }
    }

    private void extractReferences(com.fasterxml.jackson.databind.JsonNode node, ExternalIdInfoDto currentRef) {
        currentRef.setOwnerReference(node.path("ownerReference").asText(null));
        currentRef.setDealerReference(node.path("dealerReference").asText(null));
        currentRef.setChannelReference(node.path("channelReference").asText(null));
    }

}