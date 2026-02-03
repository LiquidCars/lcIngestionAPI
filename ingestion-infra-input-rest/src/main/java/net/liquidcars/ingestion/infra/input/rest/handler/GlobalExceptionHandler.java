package net.liquidcars.ingestion.infra.input.rest.handler;

import lombok.RequiredArgsConstructor;
import net.liquidcars.ingestion.domain.model.exception.LCIngestionException;
import net.liquidcars.ingestion.domain.service.context.IContextService;
import net.liquidcars.ingestion.infra.input.rest.model.LCIngestionExceptionApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static net.liquidcars.ingestion.domain.model.exception.LCTechCauseEnum.INVALID_REQUEST;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final IContextService contextService;

    @ExceptionHandler(LCIngestionException.class)
    public ResponseEntity<LCIngestionExceptionApiResponse> handleLCException(LCIngestionException ex) {

        LCIngestionExceptionApiResponse error = LCIngestionExceptionApiResponse.builder()
                .uid(contextService.getContext().getParticipantId())
                .code(ex.getErrorCode())
                .numericCode(ex.getNumericErrorCode())
                .message(ex.getMessage())
                .status(ex.getTechCause().httpStatus)
                .build();

        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getTechCause().httpStatus));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LCIngestionExceptionApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(
                LCIngestionExceptionApiResponse.builder()
                        .code(INVALID_REQUEST.name())
                        .numericCode(INVALID_REQUEST.code)
                        .message("Error input data")
                        .status(400)
                        .build()
        );
    }
}
