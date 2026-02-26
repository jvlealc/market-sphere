package io.github.jvlealc.marketsphere.products.dto.error;

public record ValidationErrorDto(
        String field,
        String error
) { }
