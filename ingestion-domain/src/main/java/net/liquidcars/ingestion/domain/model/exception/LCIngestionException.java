package net.liquidcars.ingestion.domain.model.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LCIngestionException extends RuntimeException {
    private final LCTechCauseEnum techCause;

    public LCIngestionException(LCTechCauseEnum techCause, String message) {
        super(message);
        this.techCause = techCause;
    }

    public LCIngestionException(LCTechCauseEnum techCause) {
        this.techCause = techCause;
    }

    public LCIngestionException(LCTechCauseEnum techCause, String message, Throwable cause) {
        super(message, cause);
        this.techCause = techCause;
    }

    public String getErrorCode() {
        return techCause != null ? techCause.name() : null;
    }

    public int getNumericErrorCode() {
        return techCause != null ? techCause.getCode() : 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private LCTechCauseEnum techCause = LCTechCauseEnum.INTERNAL_ERROR; // Default
        private String message;
        private Throwable cause;

        public Builder techCause(LCTechCauseEnum techCause) {
            this.techCause = techCause;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public LCIngestionException build() {
            return new LCIngestionException(techCause, message, cause);
        }
    }
}
