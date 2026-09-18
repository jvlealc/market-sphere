package io.github.jvlealc.marketsphere.customers.shared.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.customers.client.brasilapi.BrasilApiException;
import io.github.jvlealc.marketsphere.customers.client.brasilapi.PostalCodeNotFoundException;
import io.github.jvlealc.marketsphere.customers.exception.AddressNotFoundException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerAddressAlreadyExistsException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerEmailAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNationalIdAlreadyInUseException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @Nonnull MethodArgumentNotValidException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Validation Error",
                "Validation failed for one or more fields",
                servletRequest
        );

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> {
                    Map<String, String> errorDetails = new HashMap<>();
                    errorDetails.put("field", fe.getField());
                    errorDetails.put("message", fe.getDefaultMessage());
                    return errorDetails;
                })
                .toList();

        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            @Nonnull HttpMessageNotReadableException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        String detail = "Malformed JSON request.";
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            String field = invalidFormatException.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((path, fieldName) -> path + "." + fieldName)
                    .orElse("unknown");

            detail += " Field: " + field + ".";
        }

        return ResponseEntity.status(status)
                .body(createProblemDetail(
                        HttpStatus.BAD_REQUEST,
                        "Malformed JSON",
                        detail,
                        servletRequest
                ));
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            @Nonnull NoHandlerFoundException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        return ResponseEntity.status(status)
                .body(createProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Resource Not Found",
                        "The URI " + servletRequest.getRequestURI() + " does not exist on this server",
                        servletRequest
                ));

    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            @Nonnull HttpRequestMethodNotSupportedException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        String detail = "Method " + ex.getMethod() + " is not allowed on this resource.";
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            detail += " Supported methods: " + ex.getSupportedHttpMethods();
        }

        return ResponseEntity.status(status)
                .body(createProblemDetail(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "Method Not Allowed",
                        detail,
                        servletRequest
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation at [{}]: {}", request.getRequestURI(), ex.getMessage());

        String detail = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed for path or query parameters.");

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Parameter Validation Error", detail, request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handlerUnexpectedExceptions(Exception ex, HttpServletRequest request) {
        log.error(
                "Unexpected internal server error at URI: [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    // ----------- Escopo do domínio

    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail handleCustomerNotFoundException(CustomerNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Customer Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(CustomerEmailAlreadyInUseException.class)
    ProblemDetail handleEmailAlreadyInUseException(CustomerEmailAlreadyInUseException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "E-mail Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(CustomerNationalIdAlreadyInUseException.class)
    ProblemDetail handleCustomerNationalIdAlreadyInUseException(
            CustomerNationalIdAlreadyInUseException ex,
            HttpServletRequest request
    ) {
        return createProblemDetail(HttpStatus.CONFLICT, "National ID Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(AddressNotFoundException.class)
    ProblemDetail handleAddressNotFoundException(AddressNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Address Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(CustomerAddressAlreadyExistsException.class)
    ProblemDetail handleCustomerAddressAlreadyExistsException(
            CustomerAddressAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return createProblemDetail(HttpStatus.CONFLICT, "Address Conflict", ex.getMessage(), request);
    }

    // ----------- Persistência

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation at URI: [{}]: {}",
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage()
        );

        return createProblemDetail(
                HttpStatus.CONFLICT,
                "Resource Conflict",
                "The request conflicts with the current state of the resource.",
                request
        );
    }

    // ----------- Client HTTP

    @ExceptionHandler(BrasilApiException.class)
    ProblemDetail handleBrasilApiException(BrasilApiException ex, HttpServletRequest request) {
        log.warn("BrasilAPI error at URI: [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createProblemDetail(
                HttpStatus.BAD_GATEWAY,
                "External Service Communication Error",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(PostalCodeNotFoundException.class)
    ProblemDetail handlePostalCodeNotFoundException(PostalCodeNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Postal Code Not Found", ex.getMessage(), request);
    }

    // ----------- Helpers

    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("urn:error:" + status.value()));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    private ProblemDetail createInternalServerErrorProblemDetail(HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                """
                        An unexpected error has occurred. Please try again later.
                        If the error persists, please contact our support.
                        """,
                request
        );
    }
}
