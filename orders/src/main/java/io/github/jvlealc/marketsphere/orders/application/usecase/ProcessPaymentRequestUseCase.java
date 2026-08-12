package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.orders.application.model.payment.PaymentRequestReceipt;
import io.github.jvlealc.marketsphere.orders.application.ports.out.PaymentGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.service.OutboxRelayService;
import io.github.jvlealc.marketsphere.orders.application.service.PaymentRequestRegistrationService;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidOutboxMessageException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessPaymentRequestUseCase {

    private final OutboxRelayService outboxRelay;
    private final OutboxRelaySettings settings;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentRequestRegistrationService paymentRequestRegistration;

    public void execute() {
        outboxRelay.relay(
                OutboxChannel.PAYMENT,
                OutboxEventType.PAYMENT_REQUEST_REQUIRED,
                settings,
                this::deliver
        );
    }

    private void deliver(OutboxMessage message) {
        Long orderId = orderIdOf(message);

        PaymentRequestReceipt receipt = paymentGateway.requestPayment(orderId, message.getIdempotencyKey());

        paymentRequestRegistration.registerPaymentRequest(orderId, receipt);
    }

    private static Long orderIdOf(OutboxMessage message) {
        String aggregateId = message.getAggregateId();

        try {
            return Long.valueOf(aggregateId);

        } catch (NumberFormatException e) {
            throw new InvalidOutboxMessageException("Invalid order ID in outbox aggregateId: " + aggregateId, e);
        }
    }
}
