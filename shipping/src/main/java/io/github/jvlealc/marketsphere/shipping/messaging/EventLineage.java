package io.github.jvlealc.marketsphere.shipping.messaging;

import io.github.jvlealc.marketsphere.shipping.identity.UuidV7;

public record EventLineage(
        String correlationId,
        String causationId
) {

    private static final int MAX_LENGTH = 64;

    public EventLineage {
        correlationId = normalizeRequiredId(correlationId);
        causationId = normalizeOptionalId(causationId);
    }

    /**
     * Abre um fluxo novo disparado por um evento externo. O estímulo tem identidade própria — é ele que
     * vira {@code causationId} —, mas não pertence a nenhuma correlação interna.
     */
    public static EventLineage startCausedBy(String causationId) {
        return new EventLineage(
                UuidV7.generate().toString(),
                causationId
        );
    }

    public static EventLineage from(
            String correlationId,
            String causationId
    ) {
        return new EventLineage(
                isNullOrBlank(correlationId)
                        ? UuidV7.generate().toString()
                        : correlationId,
                causationId
        );
    }

    private static String normalizeRequiredId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be null or blank".formatted("correlationId"));
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("%s must not exceed %d characters".formatted("correlationId", MAX_LENGTH));
        }

        return normalized;
    }

    private static String normalizeOptionalId(String value) {
        if (isNullOrBlank(value)) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("%s must not exceed %d characters".formatted("causationId", MAX_LENGTH));
        }

        return normalized;
    }

    private static boolean isNullOrBlank(String value) {
        return value == null || value.isBlank();
    }
}