package io.github.jvlealc.marketsphere.orders.application.ports.out.product;

import java.math.BigDecimal;

public record ProductSnapshot(
        Long id,
        String name,
        BigDecimal unitPrice,
        String description,
        boolean active
) {
}