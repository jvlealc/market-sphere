package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.PaymentType;

public record PaymentInfoCommand(String metadata, PaymentType paymentType) {
}
