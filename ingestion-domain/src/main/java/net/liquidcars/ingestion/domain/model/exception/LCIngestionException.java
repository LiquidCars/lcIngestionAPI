package net.liquidcars.ingestion.domain.model.exception;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class LCIngestionException extends RuntimeException {

    private final LCTechCauseEnum techCause;

    @Builder
    public LCIngestionException(
            LCTechCauseEnum techCause,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.techCause = techCause;
    }

    public String getErrorCode() {
        return techCause != null ? techCause.name() : null;
    }

    public int getNumericErrorCode() {
        return techCause != null ? techCause.getCode() : 0;
    }
}
