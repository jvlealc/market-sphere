package io.github.jvlealc.marketsphere.orders.application.outbox.relay;

import io.github.jvlealc.marketsphere.orders.application.outbox.UndeliverableOutboxMessageException;
import io.github.jvlealc.marketsphere.orders.application.notification.OrderPaidCustomerNotification;
import io.github.jvlealc.marketsphere.orders.application.notification.OrderPaidNotification;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.outbox.payload.OutboxPayloadDeserializationException;
import io.github.jvlealc.marketsphere.orders.application.outbox.payload.OrderPaidNotificationPayload;
import io.github.jvlealc.marketsphere.orders.application.notification.NotificationPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.payload.OutboxPayloadCodecPort;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessOrderPaidNotificationUseCase {

    private final OutboxRelayService outboxRelay;
    private final OutboxRelaySettings settings;
    private final OutboxPayloadCodecPort payloadCodec;
    private final NotificationPort notificationPort;

    public void execute() {
        outboxRelay.relay(
                OutboxChannel.EMAIL,
                OutboxEventType.ORDER_PAID,
                settings,
                this::deliver
        );
    }

    private void deliver(OutboxMessage message) {
        OrderPaidNotificationPayload payload = readPayload(message);

        notificationPort.sendPaidOrderConfirmation(toNotification(payload));
    }

    private OrderPaidNotificationPayload readPayload(OutboxMessage message) {
        try {
            return payloadCodec.deserialize(
                    message.getPayload(),
                    OrderPaidNotificationPayload.class
            );

        } catch (OutboxPayloadDeserializationException contractFailure) {
            throw new UndeliverableOutboxMessageException(
                    "Stored ORDER_PAID e-mail payload of outbox message %s could not be read".formatted(message.getId()),
                    contractFailure
            );
        }
    }

    private static OrderPaidNotification toNotification(OrderPaidNotificationPayload payload) {
        return new OrderPaidNotification(
                payload.orderId(),
                payload.orderTotal(),
                new OrderPaidCustomerNotification(
                        payload.customer().customerId(),
                        payload.customer().fullName(),
                        payload.customer().email()
                )
        );
    }
}
