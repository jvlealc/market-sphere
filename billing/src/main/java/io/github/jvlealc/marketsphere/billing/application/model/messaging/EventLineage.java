package io.github.jvlealc.marketsphere.billing.application.model.messaging;

import io.github.jvlealc.marketsphere.billing.application.identity.UuidV7;

/**
 * A que fluxo o evento em processamento pertence, e qual evento o causou.
 *
 * <h2>Os dois campos não são a mesma coisa</h2>
 * {@code correlationId} identifica o <strong>fluxo</strong> — todos os eventos disparados pela mesma ação
 * original compartilham o valor. Quando o OpenTelemetry entrar, ele passa a ser populado a partir do
 * {@code trace-id} do W3C Trace Context; nunca a conviver com ele como segunda fonte de verdade.
 * <p>
 * {@code causationId} identifica o <strong>evento anterior</strong>, aquele que causou este. É linhagem de
 * evento, não de tracing: sobrevive à retenção do backend de traces e é consultável em SQL. Por isso os
 * dois coexistem em vez de um substituir o outro.
 *
 * <h2>Por que {@code String} e não {@code UUID}</h2>
 * Os dois valores chegam de fora deste serviço. Hoje são UUIDs; amanhã podem ser um {@code trace-id} de 32
 * hex ou um {@code span-id} de 16 hex — este último não cabe num {@code UUID}. Tipar identificador de
 * terceiro pelo formato que ele tem hoje é o que transformou o {@code trackingCode} em poison pill entre
 * {@code shipping} e {@code orders}.
 */
public record EventLineage(String correlationId, String causationId) {

    /**
     * Comprimento da coluna {@code varchar(64)} que persiste os dois campos. Truncar silenciosamente
     * corromperia a correlação; estourar aqui expõe o emissor que mandou algo fora do contrato.
     */
    private static final int MAX_LENGTH = 64;

    public EventLineage {
        correlationId = requireValid(correlationId, "Correlation ID");
        causationId = normalizeOptional(causationId, "Causation ID");
    }

    /**
     * Constrói a linhagem a partir do que chegou na borda de entrada.
     * <p>
     * Um {@code correlationId} ausente significa que o emissor ainda não propaga correlação — hoje é o caso
     * do {@code orders}, cujo produtor publica sem headers. Neste caso este serviço vira a <em>raiz</em> do
     * fluxo e gera o valor, em vez de gravar {@code null}. A escolha é deliberada: um trace pela metade
     * ainda permite investigar o {@code billing}.
     * Quando o {@code orders} passar a enviar o header, este méto-do só deixa de cair no ramo de geração.
     * <p>
     * Já um {@code causationId} ausente permanece nulo..
     */
    public static EventLineage from(String correlationId, String causationId) {
        return new EventLineage(
                isBlank(correlationId) ? UuidV7.generate().toString() : correlationId,
                causationId
        );
    }

    private static String requireValid(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return requireWithinMaxLength(value.trim(), fieldName);
    }

    private static String normalizeOptional(String value, String fieldName) {
        if (isBlank(value)) {
            return null;
        }

        return requireWithinMaxLength(value.trim(), fieldName);
    }

    private static String requireWithinMaxLength(String value, String fieldName) {
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "%s must not exceed %d characters".formatted(fieldName, MAX_LENGTH)
            );
        }

        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
