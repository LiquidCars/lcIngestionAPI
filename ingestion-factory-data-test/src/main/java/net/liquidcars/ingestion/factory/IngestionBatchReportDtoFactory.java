package net.liquidcars.ingestion.factory;

import net.liquidcars.ingestion.domain.model.batch.IngestionBatchReportDto;
import org.instancio.Instancio;

public class IngestionBatchReportDtoFactory {

    public static IngestionBatchReportDto getIngestionBatchReportDto() {
       return Instancio.create(IngestionBatchReportDto.class);
    }

}
