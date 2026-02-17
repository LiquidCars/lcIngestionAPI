package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionReportResponseActionMsg {
    private String ingestionReportId;
    private String result;
    private String techCause;
    private String errorMsg;
}