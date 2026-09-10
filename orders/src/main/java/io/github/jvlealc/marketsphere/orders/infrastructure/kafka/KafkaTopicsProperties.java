package io.github.jvlealc.marketsphere.orders.infrastructure.kafka;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "market-sphere.kafka.topics")
@Validated
public record KafkaTopicsProperties(
        @NotBlank String paidOrders,
        @NotBlank String billedOrders,
        @NotBlank String readyForShipmentOrders,
        @NotBlank String preparingShipmentOrders,
        @NotBlank String shippedOrders
) {
}
