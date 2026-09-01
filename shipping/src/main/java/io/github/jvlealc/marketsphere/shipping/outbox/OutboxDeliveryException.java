package io.github.jvlealc.marketsphere.shipping.outbox;

/**
 * A entrega falhou de forma <strong>retentável</strong>: broker indisponível, timeout, buffer cheio.
 * Tentar de novo mais tarde pode dar certo.
 *
 * @see UndeliverableOutboxMessageException para o caso terminal
 */
public class OutboxDeliveryException extends RuntimeException {

    public OutboxDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
