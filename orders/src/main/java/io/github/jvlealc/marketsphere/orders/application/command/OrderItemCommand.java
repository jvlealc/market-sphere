package io.github.jvlealc.marketsphere.orders.application.command;

public record OrderItemCommand(Long productId, Integer amount) {
}
