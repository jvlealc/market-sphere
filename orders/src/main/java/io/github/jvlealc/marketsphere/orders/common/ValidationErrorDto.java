package io.github.jvlealc.marketsphere.orders.common;

public record ValidationErrorDto(
        String field,
        String error
) { }
