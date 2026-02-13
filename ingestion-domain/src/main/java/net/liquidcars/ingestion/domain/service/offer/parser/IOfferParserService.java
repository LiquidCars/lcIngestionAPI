package net.liquidcars.ingestion.domain.service.offer.parser;

import net.liquidcars.ingestion.domain.model.OfferDto;
import net.liquidcars.ingestion.domain.model.batch.IngestionFormat;
import net.liquidcars.ingestion.domain.model.batch.JobDeleteExternalIdsCollector;

import java.io.InputStream;
import java.util.function.Consumer;

public interface IOfferParserService {
    void parseAndProcess(InputStream inputStream, Consumer<OfferDto> action, JobDeleteExternalIdsCollector deleteExternalIdsCollector);
    boolean supports(IngestionFormat format);
}
