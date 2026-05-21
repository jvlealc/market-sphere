package io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher;

public interface OrderPaidPublisherPort {

    void publish(OrderPaidEvent event);
}
