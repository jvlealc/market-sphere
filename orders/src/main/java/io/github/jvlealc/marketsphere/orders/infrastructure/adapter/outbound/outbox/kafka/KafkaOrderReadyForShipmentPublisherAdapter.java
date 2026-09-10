package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.outbox.kafka;

import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.OrderReadyForShipmentPublisherPort;
import io.github.jvlealc.marketsphere.orders.infrastructure.kafka.KafkaTopicsProperties;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.outbox.OutboxRelayProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class KafkaOrderReadyForShipmentPublisherAdapter implements OrderReadyForShipmentPublisherPort {

    private final KafkaOutboxPublisher publisher;
    private final KafkaTopicsProperties topics;
    private final Duration publishTimeout;

    KafkaOrderReadyForShipmentPublisherAdapter(
            KafkaOutboxPublisher publisher,
            KafkaTopicsProperties topics,
            OutboxRelayProperties props
    ) {
        this.publisher = publisher;
        this.topics = topics;
        this.publishTimeout = props.orderReadyForShipment().deliveryTimeout();
    }

    @Override
    public void publish(OutboxMessage message) {
        publisher.publish(
                message,
                OutboxEventType.ORDER_READY_FOR_SHIPMENT,
                topics.readyForShipmentOrders(),
                publishTimeout
        );
    }
}
