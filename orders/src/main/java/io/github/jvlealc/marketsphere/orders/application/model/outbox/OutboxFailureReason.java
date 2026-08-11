package io.github.jvlealc.marketsphere.orders.application.ports.out.outbox;

public final class OutboxFailureReason {

    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private static final String DEFAULT_MESSAGE = "No error message provided";

    private final String message;

    private OutboxFailureReason(String message) {
        this.message = message;
    }

    public static OutboxFailureReason of(String message) {
        if (message == null || message.isBlank()) {
            return new OutboxFailureReason(DEFAULT_MESSAGE);
        }

        String normalized = message.trim();

        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            normalized = normalized.substring(0, MAX_MESSAGE_LENGTH);
        }

        return new OutboxFailureReason(normalized);
    }

    public static OutboxFailureReason of(Throwable throwable) {
        if (throwable == null) {
            return new OutboxFailureReason(DEFAULT_MESSAGE);
        }

        String exceptionName = throwable.getClass().getSimpleName();
        String message = throwable.getMessage();

        if (message == null || message.isBlank()) {
            return of(exceptionName);
        }

        return of(exceptionName + ": " + message);
    }

    public String value() {
        return message;
    }
}
