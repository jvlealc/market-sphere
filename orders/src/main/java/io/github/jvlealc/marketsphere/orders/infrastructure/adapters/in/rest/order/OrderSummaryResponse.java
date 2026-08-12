package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(

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
        int amountItems
) { }
