package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.liquidcars.ingestion.domain.IngestionReportResponseActionResult;
import net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionReportResponseActionMsg {
    private UUID ingestionReportId;
    private IngestionReportResponseActionResult result;
    private LCTechCauseEnum techCause;
    private String errorMsg;
}