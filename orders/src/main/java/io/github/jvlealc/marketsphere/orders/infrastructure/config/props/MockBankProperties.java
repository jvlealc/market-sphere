package io.github.jvlealc.marketsphere.orders.infrastructure.config.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configurações do GATEWAY de pagamento
 * */
@Configuration
@ConfigurationProperties(prefix = "market-sphere.external-services.banking.mock-bank")
@Data
public class MockBankProperties {
    private String clientApiKey;
    private String webhookSecret;
}
