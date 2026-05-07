package io.github.jvlealc.marketsphere.orders.common;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.orders.messaging.MessagingSerializationException;
import io.github.jvlealc.marketsphere.orders.order.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.order.exception.ProductUnitPriceUnavailableException;
import io.github.jvlealc.marketsphere.orders.client.customers.CustomerClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.client.products.ProductClientNotFoundException;
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
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "An unexpected error has occurred. Please try again later.If the error persists, please contact our support team.";
    private static final String MALFORMED_JSON_TYPE_MESSAGE = "Malformed JSON request or invalid field type.";

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
                .body(ErrorResponseDto.internalServerError(INTERNAL_SERVER_ERROR_MESSAGE, httpRequest.getRequestURI()));
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

    @ExceptionHandler(ProductUnitPriceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleProductUnitPriceUnavailableException(
            final ProductUnitPriceUnavailableException exception,
            final HttpServletRequest httpRequest
    ) {
        log.error(
                "Bad request at: [{}]: {}",
                httpRequest.getRequestURI(),
                exception.getMessage(),
                exception
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST.value())
                .body(ErrorResponseDto.badRequest(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductNotFoundException(
            final OrderNotFoundException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND.value())
                .body(ErrorResponseDto.notFound(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(CustomerClientNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomerClientNotFoundException(
            final CustomerClientNotFoundException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND.value())
                .body(ErrorResponseDto.notFound(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(ProductClientNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductClientNotFoundException(
            final ProductClientNotFoundException exception,
            final HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND.value())
                .body(ErrorResponseDto.notFound(exception.getMessage(), httpRequest.getRequestURI()));
    }

    @ExceptionHandler(MessagingSerializationException.class)
    public ResponseEntity<ErrorResponseDto> handleMessagingSerializationException(
            final MessagingSerializationException exception,
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
                .body(ErrorResponseDto.internalServerError(INTERNAL_SERVER_ERROR_MESSAGE, httpRequest.getRequestURI()));
    }
}
