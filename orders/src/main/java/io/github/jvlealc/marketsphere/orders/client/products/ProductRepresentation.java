package io.github.jvlealc.marketsphere.orders.client.products;

import java.math.BigDecimal;

public record ProductRepresentation(
    Long id,
    String name,
    BigDecimal unitPrice,
    String description,
    boolean active
) { }
