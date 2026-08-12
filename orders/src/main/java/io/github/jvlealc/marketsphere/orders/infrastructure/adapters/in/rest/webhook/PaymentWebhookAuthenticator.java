package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.rest.webhook;

import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.MockBankProps;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
class PaymentWebhookAuthenticator {

    private final String expectedSecret;

    public PaymentWebhookAuthenticator(MockBankProps props) {
        this.expectedSecret = props.webhookSecret();
    }

    public void authenticate(String receivedSecret) {
        if (receivedSecret == null || receivedSecret.isBlank()) {
            throw new InvalidWebhookSecretException();
        }

        boolean isValidSecret = MessageDigest.isEqual(
                receivedSecret.getBytes(StandardCharsets.UTF_8),
                expectedSecret.getBytes(StandardCharsets.UTF_8)
        );

        if (!isValidSecret) {
            throw new InvalidWebhookSecretException();
        }
    }
}
