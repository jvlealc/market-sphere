package io.github.jvlealc.marketsphere.customers.exception.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.customers.dto.error.ErrorResponseDto;
import io.github.jvlealc.marketsphere.customers.dto.error.ValidationErrorDto;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNationalIdAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerEmailAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.client.BrasilApiException;
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

    private static final String VALIDATION_ERROR_MESSAGE = "Validation error.";
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error has occurred. Please try again later.\nIf the error persists, please contact our support team.";
    private static final String MALFORMED_JSON_TYPE_MESSAGE = "Malformed request.";

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
                .body( new ErrorResponseDto(
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        VALIDATION_ERROR_MESSAGE,
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
                .body( new ErrorResponseDto(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        INTERNAL_SERVER_ERROR_MESSAGE,
                        List.of(),
                        httpRequest.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException (
            final HttpMessageNotReadableException exception,
            final HttpServletRequest httpRequest
    ) {
        log.error("Malformed JSON or invalid field type at: [{}]: {}", httpRequest.getRequestURI(), exception.getMessage(), exception);

        String message = MALFORMED_JSON_TYPE_MESSAGE;

        if (exception.getCause() instanceof InvalidFormatException invalidFormatException) {
            String fieldName = invalidFormatException.getPath()
                    .stream()
                    .map(JsonMappingException.Reference::getFieldName) // nome do campo no JSON
                    .reduce((previous, current) -> previous + "." + current) // concatena níveis, se houver campos aninhados
                    .orElse("unknown"); // fallback se não conseguir identificar

            message += " Field: " + fieldName;
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST.value())
                .body(ErrorResponseDto.badRequest(message, httpRequest.getRequestURI()));
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductNotFoundException(
            final CustomerNotFoundException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND.value())
                .body(ErrorResponseDto.notFound(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(CustomerEmailAlreadyInUseException.class)
    public ResponseEntity<ErrorResponseDto> handleEmailAlreadyInUseException(
            final CustomerEmailAlreadyInUseException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT.value())
                .body(ErrorResponseDto.conflict(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(CustomerNationalIdAlreadyInUseException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomerNationalIdAlreadyInUseException(
            final CustomerNationalIdAlreadyInUseException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT.value())
                .body(ErrorResponseDto.conflict(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(BrasilApiException.class)
    public ResponseEntity<ErrorResponseDto> handleBrasilApiException(
            final BrasilApiException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST.value())
                .body(ErrorResponseDto.badRequest(exception.getMessage(), httpRequest.getRequestURI()));
    }
}
