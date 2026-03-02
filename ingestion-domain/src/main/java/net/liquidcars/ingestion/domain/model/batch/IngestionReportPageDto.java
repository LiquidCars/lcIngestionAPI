package net.liquidcars.ingestion.domain.model.batch;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IngestionReportPageDto {
    private List<IngestionReportDto> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer size;
    private Integer number;
    private Boolean last;
}