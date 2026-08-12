package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.messaging.kafka;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderReadyForShipmentPublisherPort;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.KafkaTopicsProps;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.OutboxRelayProps;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
class KafkaOrderReadyForShipmentPublisherAdapter implements OrderReadyForShipmentPublisherPort {

    private final KafkaOutboxPublisher publisher;
    private final KafkaTopicsProps topics;
    private final Duration publishTimeout;

    KafkaOrderReadyForShipmentPublisherAdapter(
            KafkaOutboxPublisher publisher,
            KafkaTopicsProps topics,
            OutboxRelayProps props
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
