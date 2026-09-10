package io.github.jvlealc.marketsphere.orders.application.outbox.relay;

import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxMessage;

public interface OrderPaidPublisherPort {

    void publish(OutboxMessage message);
}
