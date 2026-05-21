package io.github.jvlealc.marketsphere.orders.application.command;

import java.time.Instant;

public record HandlePaymentConfirmationCommand(
        Long orderId,
        String paymentKey,
        boolean successful,
        String observations,
        Instant paidAt
) {
}
