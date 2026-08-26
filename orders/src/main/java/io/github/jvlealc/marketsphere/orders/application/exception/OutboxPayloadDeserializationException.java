package io.github.jvlealc.marketsphere.orders.application.exception;

/**
 * O JSON gravado na coluna {@code payload} não pôde ser lido de volta para o tipo esperado.
 * <p>
 * Separada de {@code OutboxPayloadSerializationException} porque as disposições são opostas: falha de
 * escrita é bug nosso e aborta a transação antes de gravar a linha; falha de leitura significa que o
 * contrato gravado não é mais legível, e leva a mensagem a {@code DEAD}.
 */
public class OutboxPayloadDeserializationException extends ApplicationException {

    public OutboxPayloadDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
