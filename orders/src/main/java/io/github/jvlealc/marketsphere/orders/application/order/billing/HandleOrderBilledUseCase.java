package io.github.jvlealc.marketsphere.orders.application.order.billing;

import io.github.jvlealc.marketsphere.orders.application.order.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.outbox.OrderOutboxMessageFactory;
import io.github.jvlealc.marketsphere.orders.application.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.order.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HandleOrderBilledUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final OrderOutboxMessageFactory outboxFactory;

    @Transactional
    public void execute(HandleOrderBilledCommand command, EventLineage eventLineage) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(eventLineage, "eventLineage must not be null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        boolean isBilled = order.markAsBilled(command.invoiceId(), command.billedAt());

        if (isBilled) {
            Order savedOrder = orderRepository.save(order);
            outboxRepository.appendNew(
                    outboxFactory.createForOrderReadyForShipment(savedOrder, eventLineage)
            );
        }
    }
}
