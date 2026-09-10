package io.github.jvlealc.marketsphere.orders.application.outbox.relay;

import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxMessage;

public interface OrderReadyForShipmentPublisherPort {

    void publish(OutboxMessage message);
}
