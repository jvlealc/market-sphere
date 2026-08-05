package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.billing.application.exception.ApplicationException;
import io.github.jvlealc.marketsphere.billing.application.exception.InvoiceDocumentUnavailableException;
import io.github.jvlealc.marketsphere.billing.application.exception.InvoiceNotFoundException;
import io.github.jvlealc.marketsphere.billing.domain.exception.InvoiceDomainException;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.InfrastructureException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error has occurred. Please try again later.";
    private static final String INTERNAL_SERVER_ERROR_TYPE = "internal-server-error";
    public static final String VALIDATION_ERROR_TYPE = "validation-error";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @Nonnull MethodArgumentNotValidException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        ProblemDetail problemDetail = ex.getBody();
        problemDetail.setTitle("Validation Error");
        problemDetail.setDetail("Validation failed for one or more fields");
        problemDetail.setType(createUri("urn:problem:" + VALIDATION_ERROR_TYPE));
        problemDetail.setInstance(createUri(servletRequest.getRequestURI()));

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fieldError -> {
                    Map<String, String> error = new HashMap<>();
                    error.put("field", fieldError.getField());
                    error.put(
                            "message",
                            fieldError.getDefaultMessage() != null
                                    ? fieldError.getDefaultMessage()
                                    : "Invalid value"
                    );
                    return error;
                })
                .toList();

        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.now());

        return new ResponseEntity<>(problemDetail, headers, status);
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

            detail += " field: " + field + ".";
        }

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "malformed-json",
                "Malformed JSON",
                detail,
                servletRequest);

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @Override
    public ResponseEntity<Object> handleNoHandlerFoundException(
            @Nonnull NoHandlerFoundException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "resource-not-found",
                "Resource Not Found",
                "The URI " + servletRequest.getRequestURI() + " does not exist on this server",
                servletRequest
        );

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            @Nonnull NoResourceFoundException ex,
            @Nonnull HttpHeaders headers,
            @Nonnull HttpStatusCode status,
            @Nonnull WebRequest request
    ) {
        HttpServletRequest servletRequest =
                ((ServletWebRequest) request).getRequest();

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.NOT_FOUND,
                "resource-not-found",
                "Resource Not Found",
                "The requested resource does not exist",
                servletRequest
        );

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @Override
    public ResponseEntity<Object> handleHttpRequestMethodNotSupported(
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

        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                "method-not-allowed",
                "Method Not Allowed",
                detail,
                servletRequest
        );

        return new ResponseEntity<>(problemDetail, headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handlerUnexpectedExceptions(Exception ex, HttpServletRequest request) {
        log.error(
                "[Unexpected] Internal server error at: [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    // ------ Escopo de domínio ------ //
    @ExceptionHandler(InvoiceDomainException.class)
    public ProblemDetail handlerInvoiceDomainException(InvoiceDomainException ex, HttpServletRequest request) {
        log.error(
                "[Domain] Internal server error at: [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    // ------ Escopo de aplicação ------ //
    @ExceptionHandler(ApplicationException.class)
    public ProblemDetail handleApplicationException(ApplicationException ex, HttpServletRequest request) {
        log.error("[Application] Unexpected internal server error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ProblemDetail handleInvoiceNotFoundException(InvoiceNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "invoice-not-found", "Invoice Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(InvoiceDocumentUnavailableException.class)
    public ProblemDetail handleInvoiceDocumentUnavailableException(InvoiceDocumentUnavailableException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "invoice-document-unavailable", "Invoice Document Not Available", ex.getMessage(), request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        String detail = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Validation failed for path or query parameters.");

        return createProblemDetail(HttpStatus.BAD_REQUEST, VALIDATION_ERROR_TYPE, "Parameter Validation Error", detail, request);
    }

    // ------ Escopo de infraestrutura ------ //
    @ExceptionHandler(InfrastructureException.class)
    public ProblemDetail handleInfrastructureException(InfrastructureException ex, HttpServletRequest request) {
        log.error("[Infrastructure] Unexpected internal server error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    // Helpers
    private static ProblemDetail createProblemDetail(
            HttpStatus status,
            String type,
            String title,
            String detail,
            HttpServletRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(createUri("urn:problem:" + type));
        problemDetail.setInstance(createUri(request.getRequestURI()));
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    private static ProblemDetail createInternalServerErrorProblemDetail(HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TYPE,
                INTERNAL_SERVER_ERROR_TITLE,
                INTERNAL_SERVER_ERROR_DETAIL,
                request
        );
    }

    private static URI createUri(String path) {
        return URI.create(path);
    }
}
