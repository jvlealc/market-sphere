package io.github.jvlealc.marketsphere.billing.application.exception;

/**
 * O snapshot de {@code ORDER_PAID} não descreve um pedido faturável. O dado veio de fora — mensagem do
 * Kafka —, e reprocessá-lo produziria a mesma recusa, sendo terminal.
 */
public class InvalidOrderPaidSnapshotException extends UnbillableOrderException {

    public InvalidOrderPaidSnapshotException(String message) {
        super(message);
    }
}
