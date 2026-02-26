package io.github.jvlealc.marketsphere.billing.common.dto.error;

public record ValidationErrorDto(
        String field,
        String error
) { }
