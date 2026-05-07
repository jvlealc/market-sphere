package io.github.jvlealc.marketsphere.orders.notification;

import io.github.jvlealc.marketsphere.orders.client.customers.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.order.model.Order;

/**
* Sistema de notificação para clientes.
* */
public interface NotificationService {
    void sendShipmentConfirmation(CustomerRepresentation customer, Order order);
}
