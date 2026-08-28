package io.github.jvlealc.marketsphere.shipping.handler;

import io.github.jvlealc.marketsphere.shipping.outbox.payload.OrderPreparingShipmentPayload;
import io.github.jvlealc.marketsphere.shipping.outbox.OrderPreparingShipmentPublisher;
import io.github.jvlealc.marketsphere.shipping.event.ShipmentPreparationStartedApplicationEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ShipmentPreparationStartedHandler {

    private final OrderPreparingShipmentPublisher orderPreparingShipmentPublisher;

    public ShipmentPreparationStartedHandler(OrderPreparingShipmentPublisher orderPreparingShipmentPublisher) {
        this.orderPreparingShipmentPublisher = orderPreparingShipmentPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ShipmentPreparationStartedApplicationEvent event) {
        orderPreparingShipmentPublisher.publish(new OrderPreparingShipmentPayload(event.orderId()));
    }
}
