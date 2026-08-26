package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.PaymentType;

public record PaymentInfoCommand(String metadata, PaymentType paymentType) {

    public PaymentInfoCommand {
        if (metadata == null || metadata.isBlank()) {
            throw new InvalidCommandException("Metadata cannot be blank");
        }

        if (paymentType == null) {
            throw new InvalidCommandException("Payment type cannot be null");
        }

        metadata = metadata.trim();
    }
}
