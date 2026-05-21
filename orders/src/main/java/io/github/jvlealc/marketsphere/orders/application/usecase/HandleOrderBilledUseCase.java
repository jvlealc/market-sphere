package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderBilledCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class HandleOrderBilledUseCase {

    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(2);

    private final OrderRepositoryPort orderRepository;

    @Transactional
    public void execute(HandleOrderBilledCommand command) {
        validateCommandConsistency(command);

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        boolean isBilled = order.markAsBilled(command.invoiceUrl(), command.billedAt());

        if (isBilled) {
            orderRepository.save(order);
        }
    }

    private static void validateCommandConsistency(HandleOrderBilledCommand command) {
        if (command == null || command.orderId() == null) {
            throw new InvalidCommandException("Order ID is required");
        }

        if (command.invoiceUrl() == null || command.invoiceUrl().isBlank()) {
            throw new InvalidCommandException("Invoice URL is required");
        }

        if (command.billedAt() == null) {
            throw new InvalidCommandException("Billed at date is required");
        } else if (command.billedAt().isAfter(Instant.now().plus(MAX_CLOCK_SKEW))) {
            throw new InvalidCommandException("Billed at date must not be in the future");
        }
    }
}
