package net.liquidcars.ingestion.domain.model.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

@Getter
@EqualsAndHashCode(callSuper = true)
public class LCIngestionParserException extends LCIngestionException {

    private final UUID failedIdentifier;

    public LCIngestionParserException(
            LCTechCauseEnum techCause,
            String message,
            Throwable cause,
            UUID failedIdentifier
    ) {
        super(techCause, message, cause);
        this.failedIdentifier = failedIdentifier;
    }

}
