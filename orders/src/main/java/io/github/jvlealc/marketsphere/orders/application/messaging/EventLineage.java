package io.github.jvlealc.marketsphere.orders.application.messaging;

import io.github.jvlealc.marketsphere.orders.application.identity.UuidV7;

public record EventLineage(
        String correlationId,
        String causationId
) {

    private static final int MAX_LENGTH = 64;

    public EventLineage {
        correlationId = normalizeRequiredId(correlationId, "Correlation ID");
        causationId = normalizeOptionalId(causationId, "Causation ID");
    }

    public static EventLineage start() {
        return new EventLineage(
                UuidV7.generate().toString(),
                null
        );
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
                isBlank(correlationId)
                        ? UuidV7.generate().toString()
                        : correlationId,
                causationId
        );
    }

    private static String normalizeRequiredId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("%s must not be null or blank".formatted(fieldName));
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("%s must not exceed %d characters".formatted(fieldName, MAX_LENGTH));
        }

        return normalized;
    }

    private static String normalizeOptionalId(String value, String fieldName) {
        if (isBlank(value)) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("%s must not exceed %d characters".formatted(fieldName, MAX_LENGTH));
        }

        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}