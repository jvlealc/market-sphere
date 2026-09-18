package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(

        @NotNull(message = "{order.customerId.required}")
        @Positive(message = "{order.customerId.positive}")
        Long customerId,

        @NotNull(message = "{order.paymentInfo.required}")
        @Valid
        PaymentInfoRequest paymentInfo,

        @NotEmpty(message = "{order.orderItems.notEmpty}")
        @Size(max = 1500, message = "{order.orderItems.maxSize}")
        @Valid
        List<OrderItemRequest> orderItems
) {
}
