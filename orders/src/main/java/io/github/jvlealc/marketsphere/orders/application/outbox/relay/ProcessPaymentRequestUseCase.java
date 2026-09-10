package io.github.jvlealc.marketsphere.orders.application.outbox.relay;

import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.payment.PaymentRequestReceipt;
import io.github.jvlealc.marketsphere.orders.application.payment.PaymentGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.order.payment.PaymentRequestRegistrationService;
import io.github.jvlealc.marketsphere.orders.application.outbox.InvalidOutboxMessageException;
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
