package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;

import java.time.Instant;

public record HandlePaymentConfirmationCommand(
        Long orderId,
        String paymentKey,
        String paymentEventId,
        boolean successful,
        String observations,
        Instant paidAt
) {
    private static final int MAX_EVENT_ID_LENGTH = 64;

    public HandlePaymentConfirmationCommand {
        if (orderId == null) {
            throw new InvalidCommandException("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidCommandException("Order ID must be greater than zero");
        }

        if (paymentKey == null ||  paymentKey.isBlank()) {
            throw new InvalidCommandException("Payment key must not be empty");
        }

        if (successful && paidAt == null) {
            throw new InvalidCommandException("Paid At must not be null for a successful payment");
        }

        paymentKey = paymentKey.trim();
        paymentEventId = normalizePaymentEventIdWithinMaxLength(paymentEventId);
        observations = normalizeOptional(observations);
    }

    private static String normalizePaymentEventIdWithinMaxLength(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > MAX_EVENT_ID_LENGTH) {
            throw new InvalidCommandException("Payment payload ID must not exceed %d characters".formatted( MAX_EVENT_ID_LENGTH));
        }

        return normalized;
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
