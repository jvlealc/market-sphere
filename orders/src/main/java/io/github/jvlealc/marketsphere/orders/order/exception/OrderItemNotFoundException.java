package io.github.jvlealc.marketsphere.orders.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class OrderItemNotFoundException extends RuntimeException {

    public OrderItemNotFoundException(String message) {
        super(message);
    }

    public OrderItemNotFoundException(Long orderId) {
        super("Not found order item with ID: " + orderId);
    }

    public OrderItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
