package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderShippedCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidQueryException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HandleOrderShippedUseCase {

    private final OrderRepositoryPort orderRepository;

    @Transactional
    public void execute(HandleOrderShippedCommand command) {
        validateCommandConsistency(command);

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        boolean isShipped = order.markAsShipped(command.trackingCode(), command.shippedAt());

        if (isShipped) {
            orderRepository.save(order);
        }
    }

    private static void validateCommandConsistency(HandleOrderShippedCommand command) {
        if (command == null || command.orderId() == null) {
            throw new InvalidCommandException("Order ID is required");
        }

        if (command.trackingCode() == null || command.trackingCode().toString().isBlank()) {
            throw new InvalidCommandException("Tracking code is required");
        }

        if (command.shippedAt() == null) {
            throw new InvalidQueryException("Shipped at date is required");
        }
    }
}
