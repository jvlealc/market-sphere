package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderBilledCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.factory.OrderOutboxMessageFactory;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
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
        if (command == null) throw new InvalidCommandException("Handle order billed command must not be null");
        Objects.requireNonNull(eventLineage, "Event lineage must not be null");

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
