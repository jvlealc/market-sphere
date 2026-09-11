package io.github.jvlealc.marketsphere.orders.application.order.placement;

import io.github.jvlealc.marketsphere.orders.application.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.domain.order.PaymentType;

public record PaymentInfoCommand(String metadata, PaymentType paymentType) {

    public PaymentInfoCommand {
        if (paymentType == null) {
            throw new InvalidCommandException("paymentType must not be null");
        }

        metadata = (metadata == null || metadata.isBlank()) ? null : metadata.trim();
    }
}
