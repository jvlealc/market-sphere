package io.github.jvlealc.marketsphere.orders.client;

import lombok.Getter;

@Getter
public abstract class ClientException extends RuntimeException {

    private final String field;

    public ClientException(String field, String message) {
        super(message);
        this.field = field;
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
    }
}
