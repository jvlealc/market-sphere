package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.payment.webhook;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
class PaymentWebhookAuthenticator {

    private final String expectedSecret;

    public PaymentWebhookAuthenticator(MockBankProperties props) {
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
