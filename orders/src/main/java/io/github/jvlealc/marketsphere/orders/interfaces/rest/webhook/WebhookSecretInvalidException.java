package io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public final class WebhookSecretInvalidException extends RuntimeException {

    public WebhookSecretInvalidException() {
        super("The Webhook secret is invalid");
    }
}
