package io.github.jvlealc.marketsphere.shipping.service;

import io.github.jvlealc.marketsphere.shipping.publisher.OrderShippedPublisher;
import io.github.jvlealc.marketsphere.shipping.publisher.event.OrderShippedEvent;
import io.github.jvlealc.marketsphere.shipping.subscriber.event.OrderBilledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ShippingServiceImpl implements ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingServiceImpl.class);

    private final OrderShippedPublisher orderShippedPublisher;

    public ShippingServiceImpl(OrderShippedPublisher orderShippedPublisher) {
        this.orderShippedPublisher = orderShippedPublisher;
    }

    @Override
    public void processShipment(OrderBilledEvent billedEvent) {
        log.info("Processing shipment for order ID: {}", billedEvent.orderId());

        // Simulação > chamaria a API da transportadora
        UUID trackingCode = UUID.randomUUID();
        Instant shippedAt = Instant.now();

        log.info("Generated tracking code {} for order ID: {}", trackingCode, billedEvent.orderId());

        OrderShippedEvent orderShippedEvent = new OrderShippedEvent(
                billedEvent.orderId(),
                UUID.randomUUID(),
                Instant.now()
        );

        // Publica o evento no tópico do kafka
        orderShippedPublisher.publish(orderShippedEvent);
    }
}
