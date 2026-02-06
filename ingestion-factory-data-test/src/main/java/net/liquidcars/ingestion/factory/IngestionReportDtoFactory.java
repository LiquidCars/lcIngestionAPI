package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.domain.model.batch.IngestionReportDto;
import org.instancio.Instancio;

public class IngestionReportDtoFactory {

    public static IngestionReportDto getIngestionReportDto() {
       return Instancio.create(IngestionReportDto.class);
    }

}
