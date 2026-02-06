package net.liquidcars.ingestion.application.service.parser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.liquidcars.ingestion.application.service.parser.mapper.OfferParserMapper;
import net.liquidcars.ingestion.application.service.parser.model.JSON.OfferJSONModel;
import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;
import net.liquidcars.ingestion.domain.service.offer.parser.IOfferParserService;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class OfferJSONProcessor implements IOfferParserService {

    private final ObjectMapper objectMapper;
    private final OfferParserMapper offerParserMapper;

    @Override
    public boolean supports(String format) {
        return "json".equalsIgnoreCase(format);
    }

    @Override
    public void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action) {
        try (JsonParser parser = objectMapper.getFactory().createParser(inputStream)) {

            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new RuntimeException("Expected JSON Array but found: " + parser.currentToken());
            }

            while (parser.nextToken() == JsonToken.START_OBJECT) {
                OfferJSONModel model = parser.readValueAs(OfferJSONModel.class);

                if (model != null && model.isValid()) {
                    action.accept(offerParserMapper.toOfferDto(model));
                }
            }

        } catch (Exception e) {
            log.error("Streaming JSON error: {}", e.getMessage());
            throw LCIngestionException.builder()
                    .techCause(LCTechCauseEnum.CONVERSION_ERROR)
                    .message("Error during JSON stream parsing: " + e.getMessage())
                    .cause(e)
                    .build();
        }
    }
}