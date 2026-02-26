package io.github.jvlealc.marketsphere.orders.exception;

public class ProductUnitPriceUnavailableException extends RuntimeException {

    public ProductUnitPriceUnavailableException(String message) {
        super(message);
    }

    public ProductUnitPriceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
