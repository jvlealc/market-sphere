package io.github.jvlealc.marketsphere.orders.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderItemRequestDto(

        @NotNull(message = "{order.orderItems.productId.required}")
        @Positive(message = "{order.OrderItems.productId.positive}")
        Long productId,

        @NotNull(message = "{order.orderItems.amount.required}")
        @Positive(message = "{order.OrderItems.amount.positive}")
        Integer amount
) { }
