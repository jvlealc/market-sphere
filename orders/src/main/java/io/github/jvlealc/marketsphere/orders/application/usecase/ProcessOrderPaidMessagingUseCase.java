package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderPaidPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.orders.application.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessOrderPaidMessagingUseCase {

    private final OutboxRelayService outboxRelay;
    private final OutboxRelaySettings settings;
    private final OrderPaidPublisherPort orderPaidPublisher;

    public void execute() {
        outboxRelay.relay(
                OutboxChannel.MESSAGING,
                OutboxEventType.ORDER_PAID,
                settings,
                orderPaidPublisher::publish
        );
    }
}
