package io.github.jvlealc.marketsphere.orders.application.order.shipping;

import io.github.jvlealc.marketsphere.orders.application.order.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.order.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HandleOrderPreparingShipmentUseCase {

    private final OrderRepositoryPort orderRepository;

    @Transactional
    public void execute(HandleOrderPreparingShipmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Order order = orderRepository.findById(command.orderId())
                .orElseThrow(() -> new OrderNotFoundException(command.orderId()));

        boolean markedAsPreparingShipment = order.markAsPreparingShipment();

        if (markedAsPreparingShipment) {
            orderRepository.save(order);
        }
    }
}
