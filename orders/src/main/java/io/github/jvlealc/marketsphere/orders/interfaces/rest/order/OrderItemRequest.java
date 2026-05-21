package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemRequest(

        @NotNull(message = "{order.orderItems.productId.required}")
        @Positive(message = "{order.OrderItems.productId.positive}")
        Long productId,

        @NotNull(message = "{order.orderItems.amount.required}")
        @Positive(message = "{order.OrderItems.amount.positive}")
        Integer amount
) { }
