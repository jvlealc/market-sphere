package io.github.jvlealc.marketsphere.orders.infrastructure.config.props;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "market-sphere.external-services.banking.mock-bank")
@Validated
public record MockBankProps (
    @NotBlank String webhookSecret
) {
}
