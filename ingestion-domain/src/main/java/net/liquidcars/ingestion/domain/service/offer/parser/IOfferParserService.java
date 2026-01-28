package net.liquidcars.ingestion.domain.service.offer.parser;

import net.liquidcars.ingestion.domain.model.OfferDto;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public interface IOfferParserService {
    void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action);
    boolean supports(String format);
}
