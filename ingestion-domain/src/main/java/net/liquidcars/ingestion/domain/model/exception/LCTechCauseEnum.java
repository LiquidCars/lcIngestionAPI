package net.liquidcars.ingestion.domain.model.exception;

import lombok.Getter;

@Getter
public enum LCTechCauseEnum {
    // Publicación diferida
    DEFERRED_PUBLICATION(43, 202, "LCException.Global.deferred_publication"),

    // Errores de Cliente (4xx)
    INVALID_REQUEST(4, 400, "LCException.Global.invalid_request"),
    UNAUTHORIZED(11, 401, "LCException.Global.Unauthorized"),
    FORBIDDEN(20, 403, "LCException.Global.forbidden"),
    NOT_FOUND(5, 404, "LCException.Global.NotFound"),
    PAYLOAD_TOO_LARGE(7, 413, "LCException.Global.dos"),
    CONVERSION_ERROR(9, 422, "LCException.Global.ConversionError"),

    // Errores de Servidor (5xx)
    DATABASE(1, 500, "LCException.Global.database"),
    INTERNAL_ERROR(8, 500, "LCException.Global.not_specified"),
    NOT_IMPLEMENTED(18, 501, "LCException.Global.NotImplemented"),
    MESSAGING_BROKER_ERROR(43, 500, "LCException.Global.messaging_broker_error");

    public final int code;
    public final int httpStatus;
    public final String textId;

    LCTechCauseEnum(int code, int httpStatus, String textId) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.textId = textId;
    }
}
