package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.payment.webhook;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "market-sphere.external-services.banking.mock-bank")
@Validated
public record MockBankProperties(
    @NotBlank String webhookSecret
) {
}
