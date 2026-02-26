package io.github.jvlealc.marketsphere.orders.dto.error;

public record ValidationErrorDto(
        String field,
        String error
) { }
