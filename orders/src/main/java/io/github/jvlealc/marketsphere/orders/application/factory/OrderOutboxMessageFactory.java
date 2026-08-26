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
import java.util.Locale;

@Component
@RequiredArgsConstructor
public final class OrderOutboxMessageFactory {

    private static final int ORDER_PAID_EVENT_VERSION = 1;
    private static final int PAYMENT_REQUEST_REQUIRED_EVENT_VERSION = 1;
    private static final int ORDER_READY_FOR_SHIPMENT_EVENT_VERSION = 1;

    private final OutboxPayloadCodecPort payloadCodec;
    private final Clock clock;

    public OutboxMessage createForOrderPaidMessaging(Order order, EventLineage eventLineage) {
        List<OrderPaidItemPayload> itemsPayload = order.getOrderItems().stream()
                .map(OrderOutboxMessageFactory::toPayload)
                .toList();

        OrderPaidMessagingPayload payload = new OrderPaidMessagingPayload(
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
                payloadCodec.serialize(payload),
                idempotencyKeyOf(order.getId(), OutboxChannel.MESSAGING, OutboxEventType.ORDER_PAID),
                eventLineage,
                Instant.now(clock)
        );
    }

    public OutboxMessage createForOrderPaidNotification(Order order, EventLineage eventLineage) {
        OrderPaidNotificationPayload payload = new OrderPaidNotificationPayload(
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
                payloadCodec.serialize(payload),
                idempotencyKeyOf(order.getId(), OutboxChannel.EMAIL, OutboxEventType.ORDER_PAID),
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
                idempotencyKeyOf(order.getId(), OutboxChannel.PAYMENT, OutboxEventType.PAYMENT_REQUEST_REQUIRED),
                eventLineage,
                Instant.now(clock)
        );
    }

    public OutboxMessage createForOrderReadyForShipment(Order order, EventLineage eventLineage) {

        OrderReadyForShipmentPayload payload = new OrderReadyForShipmentPayload(
                order.getId(),
                order.getBilledAt(),
                new OrderReadyForShipmentPayload.OrderReadyForShipmentCustomerPayload(
                        order.getCustomerId(),
                        order.getCustomerSnapshot().fullName(),
                        order.getCustomerSnapshot().email()
                )
        );

        return OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                order.getId().toString(),
                OutboxEventType.ORDER_READY_FOR_SHIPMENT,
                ORDER_READY_FOR_SHIPMENT_EVENT_VERSION,
                order.getBilledAt(),
                OutboxChannel.MESSAGING,
                partitionKeyOf(order),
                payloadCodec.serialize(payload),
                idempotencyKeyOf(order.getId(), OutboxChannel.MESSAGING, OutboxEventType.ORDER_READY_FOR_SHIPMENT),
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

    private static String idempotencyKeyOf(Long orderId, OutboxChannel channel, OutboxEventType eventType) {
        return "%s-%s-order-%s".formatted(
                channel.name().toLowerCase(Locale.ROOT),
                eventType.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                orderId
        );
    }
}
