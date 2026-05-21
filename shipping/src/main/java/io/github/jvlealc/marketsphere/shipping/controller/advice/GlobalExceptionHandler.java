package io.github.jvlealc.marketsphere.shipping.controller.advice;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.shipping.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
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
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error has occurred. Please try again later.";

    @SuppressWarnings("NullableProblems")
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {

        ProblemDetail problemDetail = ex.getBody();
        problemDetail.setTitle("Validation Error");
        problemDetail.setDetail("Validation failed for one or more fields");
        problemDetail.setType(URI.create("urn:error:" + status.value()));

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> {
                    Map<String, String> errorDetails = new HashMap<>();
                    errorDetails.put("field", fieldError.getField());
                    errorDetails.put("message", fieldError.getDefaultMessage());
                    return errorDetails;
                })
                .toList();

        problemDetail.setProperty("errors",  errors);

        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        problemDetail.setInstance(URI.create(servletRequest.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(status).body(problemDetail);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        String detail = "Malformed JSON request.";
        if (ex.getCause() instanceof InvalidFormatException invalidFormatException) {
            String field = invalidFormatException.getPath()
                    .stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .filter(Objects::nonNull)
                    .reduce((path, fieldName) -> path + "." + fieldName)
                    .orElse("unknown");

            detail += " Field: " + field + ".";
        }

        return ResponseEntity
                .status(status)
                .body(createProblemDetail(
                        HttpStatus.BAD_REQUEST,
                        "Malformed JSON",
                        detail,
                        servletRequest
                ));
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        return ResponseEntity
                .status(status)
                .body(createProblemDetail(
                        HttpStatus.NOT_FOUND,
                        "Resource Not Found",
                        "The URI " + servletRequest.getRequestURI() + " does not exist on this server",
                        servletRequest
                ));

    }

    @SuppressWarnings("NullableProblems")
    @Override
    public ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        String detail = "Method " + ex.getMethod() + " is not allowed on this resource.";
        if (ex.getSupportedHttpMethods() != null && !ex.getSupportedHttpMethods().isEmpty()) {
            detail += " Supported methods: " + ex.getSupportedHttpMethods();
        }

        return ResponseEntity
                .status(status)
                .body(createProblemDetail(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "Method Not Allowed",
                        detail,
                        servletRequest
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.warn("Constraint violation at [{}]: {}", request.getRequestURI(), ex.getMessage());

        String detail = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed for path or query parameters.");

        return createProblemDetail(HttpStatus.BAD_REQUEST, "Parameter Validation Error", detail, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUntreatedException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected internal server error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(ApplicationException ex, HttpServletRequest request) {
        log.error("[Unhandled ApplicationException] Internal server error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    @ExceptionHandler(InvalidShipmentRequestException.class)
    public ProblemDetail handleInvalidShipmentRequestException(InvalidShipmentRequestException ex, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid Shipment Request",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    public ProblemDetail handleShipmentNotFoundException(ShipmentNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Shipment Not Found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidShipmentException.class)
    public ProblemDetail handleInvalidShipmentException(InvalidShipmentException ex, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid Shipment",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalShipmentStatusChangeException.class)
    public ProblemDetail handleIllegalShipmentStatusChangeException(IllegalShipmentStatusChangeException ex, HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.CONFLICT,
                "Shipment Status Conflict",
                ex.getMessage(),
                request
        );
    }

    private static ProblemDetail createProblemDetail(
            final HttpStatus status,
            final String title,
            final String detail,
            final HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("urn:error:" + status.value()));
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    private static ProblemDetail createInternalServerErrorProblemDetail(final HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE,
                INTERNAL_SERVER_ERROR_DETAIL,
                request
        );
    }
}
