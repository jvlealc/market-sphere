package io.github.jvlealc.marketsphere.orders.interfaces.rest.advice;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.github.jvlealc.marketsphere.orders.application.exception.*;
import io.github.jvlealc.marketsphere.orders.domain.exception.*;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.InfrastructureException;
import io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook.InvalidWebhookSecretException;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String INTERNAL_SERVER_ERROR_TITLE = "Internal Server Error";
    private static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error has occurred. Please try again later.";

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
        problemDetail.setType(URI.create("urn:error:" + status.value()));
        problemDetail.setInstance(URI.create(servletRequest.getRequestURI()));

        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fieldError -> {
                    Map<String, String> errorDetails = new HashMap<>();
                    errorDetails.put("field", fieldError.getField());
                    errorDetails.put("message", fieldError.getDefaultMessage());
                    return errorDetails;
                })
                .toList();

        problemDetail.setProperty("errors", errors);
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(status).body(problemDetail);
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
    public ResponseEntity<Object> handleNoHandlerFoundException(
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

        return ResponseEntity.status(status)
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
        log.error("[Untreated] Unexpected internal server error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    // ------ Escopo de domínio ------ //
    @ExceptionHandler(OrderDomainException.class)
    public ProblemDetail handleOrderDomainException(OrderDomainException ex, HttpServletRequest request) {
        log.error("[Domain] Unexpected internal server error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createInternalServerErrorProblemDetail(request);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ProblemDetail handleInvalidOrderException(InvalidOrderException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Order", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalOrderStatusChangeException.class)
    public ProblemDetail handleIllegalOrderStatusChangeException(IllegalOrderStatusChangeException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "Order Status Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ProblemDetail handleInvalidOrderStateException(InvalidOrderStateException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "Order State Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderRehydrationException.class)
    public ProblemDetail handleOrderRehydrationException(OrderRehydrationException ex, HttpServletRequest request) {
        log.error("Invalid persisted order state at URI [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return createInternalServerErrorProblemDetail(request);
    }

    @ExceptionHandler(InvalidOrderItemException.class)
    public ProblemDetail handleInvalidOrderItemException(InvalidOrderItemException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Order Item", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPaymentInfoException.class)
    public ProblemDetail handleInvalidPaymentInfoException(InvalidPaymentInfoException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Payment Information", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCancellationRuleException.class)
    public ProblemDetail handleInvalidCancellationRuleException(InvalidCancellationRuleException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Order Cancellation", ex.getMessage(), request);
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

    @ExceptionHandler(InvalidCommandException.class)
    public ProblemDetail handleInvalidCommandException(InvalidCommandException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Command", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ProblemDetail handleInvalidQueryException(InvalidQueryException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid Query", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFoundException(OrderNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Order Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ProblemDetail handleOrderItemNotFoundException(OrderItemNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Order Item Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(OrderItemsMissingException.class)
    public ProblemDetail handleOrderItemsMissingException(OrderItemsMissingException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNPROCESSABLE_ENTITY, "Missing Order Items", ex.getMessage(), request);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ProblemDetail handleExternalServiceException(ExternalServiceException ex, HttpServletRequest request) {
        log.error("External service error at URI [{}]: {} - {}",
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );

        return createProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "External Service Unavailable",
                "A required service is temporarily unavailable. Please try again later.",
                request
        );
    }

    @ExceptionHandler(CustomerInactiveException.class)
    public ProblemDetail handleCustomerInactiveException(CustomerInactiveException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "Customer Inactive", ex.getMessage(), request);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    public ProblemDetail handleCustomerNotFoundException(CustomerNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Customer Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ProblemDetail handleProductUnavailableException(ProductUnavailableException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.CONFLICT, "Product Unavailable", ex.getMessage(), request);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleProductNotFoundException(ProductNotFoundException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.NOT_FOUND, "Product Not Found", ex.getMessage(), request);
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

    @ExceptionHandler(InvalidWebhookSecretException.class)
    public ProblemDetail handleInvalidWebhookSecretException(InvalidWebhookSecretException ex, HttpServletRequest request) {
        return createProblemDetail(HttpStatus.UNAUTHORIZED, "Invalid Webhook Secret", ex.getMessage(), request);
    }

    // Helpers
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
