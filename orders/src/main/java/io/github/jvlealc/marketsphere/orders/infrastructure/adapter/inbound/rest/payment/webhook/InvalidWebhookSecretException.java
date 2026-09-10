package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.payment.webhook;

public final class InvalidWebhookSecretException extends RuntimeException {

    public InvalidWebhookSecretException() {
        super("The webhook secret is invalid");
    }
}
