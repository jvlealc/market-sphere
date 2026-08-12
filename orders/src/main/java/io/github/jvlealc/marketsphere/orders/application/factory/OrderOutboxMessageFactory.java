package io.github.jvlealc.marketsphere.orders.application.factory;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.*;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxAggregateType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CustomerSnapshot;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public final class OrderOutboxMessageFactory {

    private static final int ORDER_PAID_EVENT_VERSION = 1;
    private static final int PAYMENT_REQUEST_REQUIRED_EVENT_VERSION = 1;

    private final OutboxPayloadCodecPort payloadCodec;
    private final Clock clock;

    public OutboxMessage createForOrderPaidMessaging(Order order, EventLineage eventLineage) {
        List<OrderPaidItemPayload> itemsPayload = order.getOrderItems().stream()
                .map(OrderOutboxMessageFactory::toPayload)
                .toList();

        OrderPaidMessagingPayload event = new OrderPaidMessagingPayload(
                order.getId(),
                toPayload(order.getCustomerId(), order.getCustomerSnapshot()),
                order.getOrderDate(),
                order.getTotal(),
                order.getObservations(),
                itemsPayload
        );

        return OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                order.getId().toString(),
                OutboxEventType.ORDER_PAID,
                ORDER_PAID_EVENT_VERSION,
                order.getPaidAt(),
                OutboxChannel.MESSAGING,
                partitionKeyOf(order),
                payloadCodec.serialize(event),
                idempotencyKeyOf(order.getId(), OutboxChannel.MESSAGING),
                eventLineage,
                Instant.now(clock)
        );
    }

    public OutboxMessage createForOrderPaidNotification(Order order, EventLineage eventLineage) {
        OrderPaidNotificationPayload event = new OrderPaidNotificationPayload(
                order.getId(),
                order.getTotal(),
                new OrderPaidCustomerNotificationPayload(
                        order.getCustomerId(),
                        order.getCustomerSnapshot().fullName(),
                        order.getCustomerSnapshot().email()
                )
        );

        return OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                order.getId().toString(),
                OutboxEventType.ORDER_PAID,
                ORDER_PAID_EVENT_VERSION,
                order.getPaidAt(),
                OutboxChannel.EMAIL,
                null,
                payloadCodec.serialize(event),
                idempotencyKeyOf(order.getId(), OutboxChannel.EMAIL),
                eventLineage,
                Instant.now(clock)
        );
    }

    public OutboxMessage createForPaymentRequest(Order order, EventLineage eventLineage) {
        PaymentRequestPayload event = new PaymentRequestPayload(order.getId());

        return OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                order.getId().toString(),
                OutboxEventType.PAYMENT_REQUEST_REQUIRED,
                PAYMENT_REQUEST_REQUIRED_EVENT_VERSION,
                order.getOrderDate(),
                OutboxChannel.PAYMENT,
                null,
                payloadCodec.serialize(event),
                idempotencyKeyOf(order.getId(), OutboxChannel.PAYMENT),
                eventLineage,
                Instant.now(clock)
        );
    }

    private static OrderPaidCustomerPayload toPayload(Long customerId, CustomerSnapshot customer) {
        return new OrderPaidCustomerPayload(
                customerId,
                customer.fullName(),
                customer.nationalId(),
                customer.email(),
                customer.phoneNumber(),
                customer.postalCode(),
                customer.street(),
                customer.houseNumber(),
                customer.complement(),
                customer.neighborhood(),
                customer.city(),
                customer.state(),
                customer.country()
        );
    }

    private static OrderPaidItemPayload toPayload(OrderItem item) {
        return new OrderPaidItemPayload(
                item.getProductId(),
                item.getProductName(),
                item.getAmount(),
                item.getUnitPrice()
        );
    }

    private static String partitionKeyOf(Order order) {
        return order.getId().toString();
    }

    private static String idempotencyKeyOf(Long orderId, OutboxChannel channel) {
        return switch (channel) {
            case MESSAGING, EMAIL -> channel.name().toLowerCase() + "-order-paid-order-%s".formatted(orderId);
            case PAYMENT ->  channel.name().toLowerCase() + "-request-order-%s".formatted(orderId);
        };
    }
}
