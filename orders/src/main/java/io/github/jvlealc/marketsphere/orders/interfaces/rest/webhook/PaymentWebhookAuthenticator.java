package io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PaymentWebhookAuthenticator {

    private final String expectedSecret;

    public PaymentWebhookAuthenticator(
            @Value("${market-sphere.external-services.banking.mock-bank.webhook-secret}") String expectedSecret
    ) {
        this.expectedSecret = expectedSecret;
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
