package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.exception.UndeliverableOutboxMessageException;
import io.github.jvlealc.marketsphere.orders.application.model.notification.OrderPaidCustomerNotification;
import io.github.jvlealc.marketsphere.orders.application.model.notification.OrderPaidNotification;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.orders.application.exception.OutboxPayloadDeserializationException;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.OrderPaidNotificationPayload;
import io.github.jvlealc.marketsphere.orders.application.ports.out.NotificationPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.orders.application.service.OutboxRelayService;
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
