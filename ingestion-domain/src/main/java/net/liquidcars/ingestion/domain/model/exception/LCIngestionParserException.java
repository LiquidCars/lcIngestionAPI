package net.liquidcars.ingestion.domain.model.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.liquidcars.ingestion.domain.model.ExternalIdInfoDto;

@Getter
@EqualsAndHashCode(callSuper = true)
public class LCIngestionParserException extends LCIngestionException {

    private final ExternalIdInfoDto failedIdentifier;

    public LCIngestionParserException(
            LCTechCauseEnum techCause,
            String message,
            Throwable cause,
            ExternalIdInfoDto failedIdentifier
    ) {
        super(techCause, message, cause);
        this.failedIdentifier = failedIdentifier;
    }

}
