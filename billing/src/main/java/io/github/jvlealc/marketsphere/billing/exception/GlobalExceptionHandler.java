package io.github.jvlealc.marketsphere.billing.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.billing.bucket.exception.StorageAccessException;
import io.github.jvlealc.marketsphere.billing.common.dto.error.ErrorResponseDto;
import io.github.jvlealc.marketsphere.billing.common.dto.error.ValidationErrorDto;
import io.github.jvlealc.marketsphere.billing.translator.MessageTranslator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String VALIDATION_ERROR_MESSAGE_CODE = "validation.error";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE_CODE = "internal.server.error";
    private static final String MALFORMED_JSON_TYPE_MESSAGE_CODE = "malformed.json.type";

    private final MessageTranslator translator;

    public GlobalExceptionHandler(MessageTranslator translator) {
        this.translator = translator;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException exception,
            final HttpServletRequest httpRequest
    ) {
        log.error("Validation error at: [{}]: {}", httpRequest.getRequestURI(), exception.toString(), exception);

        List<FieldError> errors = exception.getFieldErrors();
        List<ValidationErrorDto> validationErrorsDto = errors.stream()
                .map(error -> new ValidationErrorDto(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .body(ErrorResponseDto.unprocessableEntity(
                        translator.translate(VALIDATION_ERROR_MESSAGE_CODE),
                        validationErrorsDto,
                        httpRequest.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handlerUntreatedExceptions(
            final Exception exception,
            final HttpServletRequest httpRequest
    ) {
        log.error(
                "Internal server error at: [{}]: {} - {}",
                httpRequest.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(ErrorResponseDto.internalServerError(
                        translator.translate(INTERNAL_SERVER_ERROR_MESSAGE_CODE),
                        httpRequest.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException (
            final HttpMessageNotReadableException exception,
            final HttpServletRequest httpRequest
    ) {
        log.error("Malformed JSON or invalid field type at: [{}]: {}", httpRequest.getRequestURI(), exception.getMessage(), exception);
        String malformedJsonMessage = MALFORMED_JSON_TYPE_MESSAGE_CODE;
        if (exception.getCause() instanceof InvalidFormatException invalidFormatException) {
            String fieldName = invalidFormatException.getPath()
                    .stream()
                    .map(JsonMappingException.Reference::getFieldName) // nome do campo no JSON
                    .reduce((previous, current) -> previous + "." + current) // concatena níveis, se houver campos aninhados
                    .orElse("unknown"); // fallback se não conseguir identificar

            malformedJsonMessage = translator.translate(malformedJsonMessage, fieldName);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST.value())
                .body(ErrorResponseDto.badRequest(malformedJsonMessage, httpRequest.getRequestURI()));
    }

    @ExceptionHandler(StorageAccessException.class)
    public ResponseEntity<ErrorResponseDto> handleStorageAccessException(
            final StorageAccessException exception,
            final HttpServletRequest httpRequest
    ) {
        log.error(
                "Internal server error at: [{}] during message serialization: {}",
                httpRequest.getRequestURI(),
                exception.getMessage(),
                exception
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .body(ErrorResponseDto.internalServerError(
                        translator.translate(INTERNAL_SERVER_ERROR_MESSAGE_CODE),
                        httpRequest.getRequestURI()
                ));
    }
}
