package io.github.jvlealc.marketsphere.orders.infrastructure.exception;

/**
 * Exceção lançada quando ocorre um erro ao enviar email
 */
public class NotificationException extends InfraException {

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
