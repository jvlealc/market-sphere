package io.github.jvlealc.marketsphere.products.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductRequestDto(

    @NotBlank(message = "{product.name.required}")
    @Size(min = 1, max = 150, message = "{product.name.size}")
    String name,

    @NotNull(message = "{product.unitPrice.required}")
    @PositiveOrZero(message = "product.unitPrice.positiveOrZero")
    BigDecimal unitPrice,

    @NotBlank(message = "{product.description.required}")
    @Size(min = 5, max = 10000, message = "product.description.size")
    String description
) { }
