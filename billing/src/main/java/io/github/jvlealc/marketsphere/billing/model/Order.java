package io.github.jvlealc.marketsphere.billing.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        Long orderId,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss",
                timezone = "America/Sao_Paulo"
        )
        Instant orderDate,
        String orderObservations,
        Customer customer,
        List<OrderItem> orderItems,
        BigDecimal total
) { }
