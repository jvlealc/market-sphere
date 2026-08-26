package io.github.jvlealc.marketsphere.orders.application.model.outbox.payload;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validação as invariantes comuns aos payloads publicados.
 */
final class PayloadValidation {

    private PayloadValidation() {
    }

    static Long requiredId(Long value, String fieldName) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(fieldName + " must be a positive identifier");
        }

        return value;
    }

    static Integer requiredQuantity(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }

        return value;
    }

    static BigDecimal requiredAmount(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        if (value.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }

        return value;
    }

    static String requiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value.trim();
    }

    static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static <T> T required(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }

        return value;
    }

    static <T> List<T> requiredItems(List<T> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must contain at least one item");
        }

        return List.copyOf(values);
    }
}
