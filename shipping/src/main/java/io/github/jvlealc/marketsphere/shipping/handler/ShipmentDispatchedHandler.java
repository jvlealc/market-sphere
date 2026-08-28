package io.github.jvlealc.marketsphere.shipping.handler;

import io.github.jvlealc.marketsphere.shipping.outbox.payload.OrderShippedPayload;
import io.github.jvlealc.marketsphere.shipping.outbox.OrderShippedPublisher;
import io.github.jvlealc.marketsphere.shipping.shipment.notification.ShipmentConfirmationEmailService;
import io.github.jvlealc.marketsphere.shipping.event.ShipmentDispatchedApplicationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ShipmentDispatchedHandler {

    private final OrderShippedPublisher orderShippedPublisher;
    private final ShipmentConfirmationEmailService shipmentConfirmationEmailService;

    public ShipmentDispatchedHandler(
            OrderShippedPublisher orderShippedPublisher,
            ShipmentConfirmationEmailService shipmentConfirmationEmailService
            ) {
        this.orderShippedPublisher = orderShippedPublisher;
        this.shipmentConfirmationEmailService = shipmentConfirmationEmailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ShipmentDispatchedApplicationEvent event) {
        orderShippedPublisher.publish(new OrderShippedPayload(
                event.orderId(),
                event.trackingCode(),
                event.shippedAt()
        ));

        shipmentConfirmationEmailService.sendIfNecessary(event.shipmentId());
    }
}
