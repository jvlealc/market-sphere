package io.github.jvlealc.marketsphere.billing.application.exception;

/**
 * Marca a falha <strong>terminal</strong>: o pedido não é faturável, e reprocessar o mesmo evento levaria
 * à mesma recusa. É a única família de exceções que leva a nota a {@code FAILED}.
 * <p>
 * Existe separada de {@link ApplicationException} de propósito. {@code ApplicationException} responde
 * "de qual camada veio"; esta responde "vale a pena tentar de novo". São perguntas diferentes, e usar a
 * primeira como resposta da segunda funciona só enquanto houver uma subclasse — na segunda, uma falha
 * recuperável seria capturada e engolida como se fosse definitiva.
 */
public abstract class UnbillableOrderException extends ApplicationException {

    protected UnbillableOrderException(String message) {
        super(message);
    }

    protected UnbillableOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
