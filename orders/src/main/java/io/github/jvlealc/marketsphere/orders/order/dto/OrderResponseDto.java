package io.github.jvlealc.marketsphere.orders.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.jvlealc.marketsphere.orders.order.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponseDto(

        Long id,
        Long customerId,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss",
                timezone = "America/Sao_Paulo"
        )
        Instant orderDate,
        String observations,
        OrderStatus status,
        BigDecimal total,
        UUID trackingCode,
        int amountItems
) { }
