package io.github.jvlealc.marketsphere.orders.infrastructure.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "market-sphere.internal-services.customers.security")
@Validated
public record CustomerClientProps(@NotBlank String apiKey) {
}
