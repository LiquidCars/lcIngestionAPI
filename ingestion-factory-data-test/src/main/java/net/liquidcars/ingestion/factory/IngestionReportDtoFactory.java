package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import org.instancio.Instancio;

public class IngestionReportDtoFactory {

    public static IngestionBatchReportDto getIngestionReportDto() {
       return Instancio.create(IngestionBatchReportDto.class);
    }

}
