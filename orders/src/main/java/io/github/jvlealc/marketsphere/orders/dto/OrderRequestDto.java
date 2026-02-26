package io.github.jvlealc.marketsphere.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderRequestDto(

        @NotNull(message = "{order.customerId.required}")
        @Positive(message = "{order.customerId.positive}")
        Long customerId,

        @NotNull(message = "{order.paymentInfo.required}")
        @Valid
        PaymentInfoRequestDto paymentInfo,

        @NotEmpty(message = "{order.orderItems.notEmpty}")
        @Valid
        List<OrderItemRequestDto> orderItems
) {
    public OrderRequestDto {
        orderItems = List.copyOf(orderItems); // garante imutabilidade
    }
}
