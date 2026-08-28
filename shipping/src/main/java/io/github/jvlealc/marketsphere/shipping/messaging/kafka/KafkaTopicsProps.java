package io.github.jvlealc.marketsphere.shipping.messaging.kafka;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "market-sphere.kafka.topics")
@Validated
public record KafkaTopicsProps(
        @NotBlank String readyForShipmentOrders,
        @NotBlank String preparingShipmentOrders,
        @NotBlank String shippedOrders
) {
}
