package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.client.feign.product;

import java.math.BigDecimal;

public record ProductRepresentation(
    Long id,
    String name,
    BigDecimal unitPrice,
    String description,
    boolean active
) { }
