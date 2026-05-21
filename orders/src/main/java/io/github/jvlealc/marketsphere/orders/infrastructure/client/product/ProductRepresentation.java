package io.github.jvlealc.marketsphere.orders.infrastructure.client.product;

import java.math.BigDecimal;

public record ProductRepresentation(
    Long id,
    String name,
    BigDecimal unitPrice,
    String description,
    boolean active
) { }
