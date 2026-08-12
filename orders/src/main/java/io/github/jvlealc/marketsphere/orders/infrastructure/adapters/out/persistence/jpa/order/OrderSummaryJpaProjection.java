package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.time.Instant;

public interface OrderSummaryJpaProjection {

    Long getId();

    Long getCustomerId();

    Instant getOrderDate();

    String getObservations();

    String getStatus();

    BigDecimal getTotal();

    Integer getAmountItems();
}
