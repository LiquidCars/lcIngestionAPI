package net.liquidcars.ingestion.infra.input.rest.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LCIngestionExceptionApiResponse {
    private String uid;
    private String code;
    private int numericCode;
    private String message;
    private int status;
}
