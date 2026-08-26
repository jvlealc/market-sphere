package io.github.jvlealc.marketsphere.orders.application.ports.out;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;

public interface OrderPaidPublisherPort {

    void publish(OutboxMessage message);
}
