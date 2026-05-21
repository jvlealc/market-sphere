package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.exception.OrderItemsMissingException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidQueryException;
import io.github.jvlealc.marketsphere.orders.application.output.OrderSummaryOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderQueryPort;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderSummaryByIdQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetOrderSummaryUseCase {

    private final OrderQueryPort orderQueryPort;

    public OrderSummaryOutput execute(GetOrderSummaryByIdQuery query) {
        if (query == null || query.orderId() == null) {
            throw new InvalidQueryException("Order ID is required");
        }

        OrderSummaryOutput order = orderQueryPort.findOrderSummaryById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        if (order.amountItems() <= 0) {
            throw new OrderItemsMissingException(order.id());
        }

        return order;
    }
}
