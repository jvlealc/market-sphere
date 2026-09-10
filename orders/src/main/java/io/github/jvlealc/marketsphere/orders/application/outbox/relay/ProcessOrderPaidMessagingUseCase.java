package io.github.jvlealc.marketsphere.orders.application.outbox.relay;

import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxEventType;
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
