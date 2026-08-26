package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderReadyForShipmentPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.service.OutboxRelayService;
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
