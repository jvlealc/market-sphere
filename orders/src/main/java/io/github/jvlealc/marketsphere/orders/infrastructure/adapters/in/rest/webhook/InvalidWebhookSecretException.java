package io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook;

public final class InvalidWebhookSecretException extends RuntimeException {

    public InvalidWebhookSecretException() {
        super("The webhook secret is invalid");
    }
}
