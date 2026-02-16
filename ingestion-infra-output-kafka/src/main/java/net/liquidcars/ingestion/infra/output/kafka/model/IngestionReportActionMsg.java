package net.liquidcars.ingestion.infra.output.kafka.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionReportActionMsg {
    private String jobId;
}