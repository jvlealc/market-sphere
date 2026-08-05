package io.github.jvlealc.marketsphere.billing.application.model.outbox;

/**
 * O motivo pelo qual uma mensagem da outbox não foi entregue, no formato em que vai para a coluna
 * {@code failure_reason}.
 */
public final class OutboxFailureReason {

    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private static final String DEFAULT_MESSAGE = "No error message provided";
    private static final String CAUSE_SEPARATOR = " <- ";

    /**
     * As exceções deste projeto embrulham no máximo duas ou três vezes; o limite existe apenas para tornar
     * impossível uma cadeia patológica esgotar o espaço da coluna com um único stack de causas.
     */
    private static final int MAX_CAUSE_DEPTH = 5;

    private final String message;

    private OutboxFailureReason(String message) {
        this.message = message;
    }

    public static OutboxFailureReason of(String message) {
        if (message == null || message.isBlank()) {
            return new OutboxFailureReason(DEFAULT_MESSAGE);
        }

        String normalized = message.trim();

        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            normalized = normalized.substring(0, MAX_MESSAGE_LENGTH);
        }

        return new OutboxFailureReason(normalized);
    }

    /**
     * Percorre a cadeia de causas, não apenas o topo.
     * <p>
     * As exceções que chegam aqui são de tradução — {@code OutboxDeliveryException} embrulha o que a
     * tecnologia lançou —, e a mensagem do topo descreve <em>o que</em> se tentava fazer, nunca <em>por
     * quê</em> falhou. Registrando só o topo, broker fora do ar, serializer quebrado e buffer cheio
     * produziriam exatamente a mesma linha, e a coluna que existe para explicar a falha explicaria apenas
     * que houve uma.
     */
    public static OutboxFailureReason of(Throwable throwable) {
        if (throwable == null) {
            return new OutboxFailureReason(DEFAULT_MESSAGE);
        }

        return of(describeCauseChain(throwable));
    }

    private static String describeCauseChain(Throwable throwable) {
        StringBuilder description = new StringBuilder();

        Throwable current = throwable;
        int depth = 0;

        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (depth > 0) {
                description.append(CAUSE_SEPARATOR);
            }

            description.append(current.getClass().getSimpleName());

            String message = current.getMessage();

            if (message != null && !message.isBlank()) {
                description.append(": ").append(message.strip());
            }

            // Exceção cuja causa é ela mesma existe, e sem esta guarda o laço só terminaria pela
            // profundidade máxima, repetindo a mesma entrada cinco vezes.
            current = current.getCause() == current ? null : current.getCause();
            depth++;
        }

        return description.toString();
    }

    public String value() {
        return message;
    }
}
