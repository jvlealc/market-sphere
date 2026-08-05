package io.github.jvlealc.marketsphere.billing.application.exception;

/**
 * A mensagem da outbox <strong>não pode</strong> ser entregue, e tentar de novo não muda o resultado:
 * payload que não desserializa, contrato violado, destinatário estruturalmente inválido.
 * <p>
 * Leva a linha direto a {@code DEAD}, sem consumir a fila de tentativas. É a mesma assimetria que
 * {@link UnbillableOrderException} resolve no caminho de entrada, aplicada agora ao caminho de saída: uma
 * falha de contrato tratada como transitória gasta cinco tentativas e vários minutos de backoff para chegar
 * ao mesmo lugar, poluindo a métrica de retentativa com algo que nunca teve chance.
 * <p>
 * O default continua sendo o oposto — {@link OutboxDeliveryException}, retentável. Só se marca como
 * terminal o que se sabe classificar; o não classificado é retentado.
 */
public class UndeliverableOutboxMessageException extends ApplicationException {

    public UndeliverableOutboxMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
