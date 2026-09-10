package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.order.jpa;

import io.github.jvlealc.marketsphere.orders.infrastructure.InfrastructureException;

public class OrderPersistenceException extends InfrastructureException {

    public OrderPersistenceException(String message) {
        super(message);
    }
}
