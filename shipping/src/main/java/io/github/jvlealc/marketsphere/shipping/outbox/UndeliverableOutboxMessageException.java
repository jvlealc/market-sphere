package io.github.jvlealc.marketsphere.shipping.outbox;

/**
 * A mensagem da outbox <strong>não pode</strong> ser entregue, e tentar de novo não muda o resultado:
 * payload que não desserializa, contrato violado, destinatário estruturalmente inválido.
 * <p>
 * Leva a linha direto a {@code DEAD}, sem consumir a fila de tentativas — uma falha de contrato tratada
 * como transitória gastaria cinco tentativas e vários minutos de backoff para chegar ao mesmo lugar,
 * poluindo a métrica de retentativa com algo que nunca teve chance.
 * <p>
 * O default continua sendo o oposto — {@link OutboxDeliveryException}, retentável. Só se marca como
 * terminal o que se sabe classificar; o não classificado é retentado.
 */
public class UndeliverableOutboxMessageException extends RuntimeException {

    public UndeliverableOutboxMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
