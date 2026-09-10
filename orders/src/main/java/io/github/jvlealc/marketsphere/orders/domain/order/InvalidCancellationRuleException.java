package io.github.jvlealc.marketsphere.orders.domain.order;

public class InvalidCancellationRuleException extends OrderDomainException {

    public InvalidCancellationRuleException(String message) {
        super(message);
    }
}
