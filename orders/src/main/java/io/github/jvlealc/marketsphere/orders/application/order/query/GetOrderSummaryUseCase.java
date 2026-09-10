package io.github.jvlealc.marketsphere.orders.application.order.query;

import io.github.jvlealc.marketsphere.orders.application.order.OrderNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class GetOrderSummaryUseCase {

    private final OrderQueryPort orderQueryPort;

    public OrderSummaryOutput execute(GetOrderSummaryByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        OrderSummaryOutput order = orderQueryPort.findOrderSummaryById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        if (order.amountItems() <= 0) {
            throw new OrderItemsMissingException(order.id());
        }

        return order;
    }
}
