package io.github.jvlealc.marketsphere.orders.service.notification;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.model.Order;

/**
* Sistema de notificação por Email para clientes.
* */
public interface EmailService {
    void sendShipmentConfirmationEmail(CustomerRepresentation customer, Order order);
}
