package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderPreparingShipmentCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class HandleOrderPreparingShipmentUseCase {

    private final OrderRepositoryPort orderRepository;

    @Transactional
    public void execute(HandleOrderPreparingShipmentCommand command) {
        if (command == null || command.orderId() == null) {
            throw new InvalidCommandException("Order ID is required");
        }

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        boolean markedAsPreparingShipment = order.markAsPreparingShipment();

        if (markedAsPreparingShipment) {
            orderRepository.save(order);
        }
    }
}
