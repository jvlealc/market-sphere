package io.github.jvlealc.marketsphere.orders.order.exception;

public class ProductUnitPriceUnavailableException extends RuntimeException {

    public ProductUnitPriceUnavailableException(String message) {
        super(message);
    }

    public ProductUnitPriceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
