package io.github.jvlealc.marketsphere.customers.dto.error;

public record ValidationErrorDto(
        String field,
        String error
) { }
