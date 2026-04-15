package io.github.jvlealc.marketsphere.orders.order.exception;

/**
 * Exceção lançada quando ocorre um erro de processamento
 * de pedidos faturados.
 */
public class OrderBilledProcessingException extends RuntimeException {
    public OrderBilledProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
