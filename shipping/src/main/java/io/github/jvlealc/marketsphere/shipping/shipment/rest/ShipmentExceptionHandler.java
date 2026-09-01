package io.github.jvlealc.marketsphere.shipping.shipment.rest;

import io.github.jvlealc.marketsphere.shipping.rest.ProblemDetailFactory;
import io.github.jvlealc.marketsphere.shipping.shipment.IllegalShipmentStatusChangeException;
import io.github.jvlealc.marketsphere.shipping.shipment.InvalidShipmentException;
import io.github.jvlealc.marketsphere.shipping.shipment.ShipmentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ShipmentController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class ShipmentExceptionHandler {

    private final ProblemDetailFactory problemDetailFactory;

    ShipmentExceptionHandler(ProblemDetailFactory problemDetailFactory) {
        this.problemDetailFactory = problemDetailFactory;
    }

    @ExceptionHandler(InvalidShipmentRequestException.class)
    ProblemDetail handleInvalidShipmentRequestException(InvalidShipmentRequestException ex, HttpServletRequest request) {
        return problemDetailFactory.create(
                HttpStatus.BAD_REQUEST,
                "Invalid Shipment Request",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ShipmentNotFoundException.class)
    ProblemDetail handleShipmentNotFoundException(ShipmentNotFoundException ex, HttpServletRequest request) {
        return problemDetailFactory.create(
                HttpStatus.NOT_FOUND,
                "Shipment Not Found",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidShipmentException.class)
    ProblemDetail handleInvalidShipmentException(InvalidShipmentException ex, HttpServletRequest request) {
        return problemDetailFactory.create(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Invalid Shipment",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalShipmentStatusChangeException.class)
    ProblemDetail handleIllegalShipmentStatusChangeException(IllegalShipmentStatusChangeException ex, HttpServletRequest request) {
        return problemDetailFactory.create(
                HttpStatus.CONFLICT,
                "Shipment Status Conflict",
                ex.getMessage(),
                request
        );
    }
}
