package io.github.jvlealc.marketsphere.orders.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String message) {
        super(message);
    }

    public OrderNotFoundException(Long orderId) {
        super("Not found order with ID: " + orderId);
    }

    public OrderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
