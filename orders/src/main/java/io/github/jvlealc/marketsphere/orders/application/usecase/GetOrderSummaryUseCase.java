package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.exception.OrderItemsMissingException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.output.OrderSummaryOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderQueryPort;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderSummaryByIdQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class GetOrderSummaryUseCase {

    private final OrderQueryPort orderQueryPort;

    public OrderSummaryOutput execute(GetOrderSummaryByIdQuery query) {
        Objects.requireNonNull(query, "Get order summary by ID query is required");

        OrderSummaryOutput order = orderQueryPort.findOrderSummaryById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        if (order.amountItems() <= 0) {
            throw new OrderItemsMissingException(order.id());
        }

        return order;
    }
}
