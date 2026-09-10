package io.github.jvlealc.marketsphere.orders.application.outbox.relay;

import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessOrderReadyForShipmentUseCase {

    private final OutboxRelayService outboxRelay;
    private final OutboxRelaySettings settings;
    private final OrderReadyForShipmentPublisherPort orderReadyForShipmentPublisher;

    public void execute() {
        outboxRelay.relay(
                OutboxChannel.MESSAGING,
                OutboxEventType.ORDER_READY_FOR_SHIPMENT,
                settings,
                orderReadyForShipmentPublisher::publish
        );
    }
}
