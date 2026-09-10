package io.github.jvlealc.marketsphere.orders.application.order.query;

import io.github.jvlealc.marketsphere.orders.application.order.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.order.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.order.Order;
import io.github.jvlealc.marketsphere.orders.domain.order.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public final class GetOrderDetailsUseCase {

    private final OrderRepositoryPort orderRepository;

    public OrderDetailsOutput execute(GetOrderDetailsByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Order order = orderRepository.findWithDetailsById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        return toOutput(order);
    }

    private static OrderDetailsOutput toOutput(Order order) {
        List<OrderItemDetailsOutput> orderItems = order.getOrderItems()
                .stream()
                .map(GetOrderDetailsUseCase::toItemOutput)
                .toList();

        return new OrderDetailsOutput(
                order.getId(),
                order.getCustomerId(),
                order.getCustomerSnapshot(),
                order.getOrderDate(),
                order.getPaidAt(),
                order.getBilledAt(),
                order.getShippedAt(),
                order.getTotal(),
                order.getStatus(),
                order.getObservations(),
                order.getInvoiceId(),
                order.getTrackingCode(),
                orderItems
        );
    }

    private static OrderItemDetailsOutput toItemOutput(OrderItem orderItem) {
        return new OrderItemDetailsOutput(
                orderItem.getProductId(),
                orderItem.getProductName(),
                orderItem.getAmount(),
                orderItem.getUnitPrice()
        );
    }
}
