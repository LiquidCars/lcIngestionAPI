package net.liquidcars.ingestion.domain.model.exception;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class LCIngestionParserException extends LCIngestionException {

    private final String failedIdentifier;

    public LCIngestionParserException(
            LCTechCauseEnum techCause,
            String message,
            Throwable cause,
            String failedIdentifier
    ) {
        super(techCause, message, cause);
        this.failedIdentifier = failedIdentifier;
    }

}
