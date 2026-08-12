package io.github.jvlealc.marketsphere.orders.application.exception;

/**
 * A entrega de uma mensagem da outbox falhou de forma <strong>retentável</strong>: broker indisponível,
 * SMTP recusando conexão, armazenamento fora do ar. Tentar de novo mais tarde pode dar certo.
 * <p>
 * Mora na camada de aplicação porque é o contrato de falha das <strong>portas</strong> de entrega: os
 * adaptadores traduzem para cá o que a tecnologia deles lança, e o relay captura um tipo só.
 *
 * @see UndeliverableOutboxMessageException para o caso terminal
 */
public class OutboxDeliveryException extends ApplicationException {

    public OutboxDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
