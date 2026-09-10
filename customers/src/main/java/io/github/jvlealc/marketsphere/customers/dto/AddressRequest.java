package io.github.jvlealc.marketsphere.customers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "{address.postalCode.required}")
        @Pattern(regexp = "\\d{8}", message = "{address.postalCode.invalid}")
        String postalCode,

        @NotBlank(message = "{address.street.required}")
        @Size(max = 100, message = "{address.street.size}")
        String street,

        @NotBlank(message = "{address.houseNumber.required}")
        @Size(max = 10, message = "{address.houseNumber.size}")
        String houseNumber,

        @Size(max = 50, message = "{address.complement.size}")
        String complement,

        @Size(max = 100, message = "{address.neighborhood.size}")
        String neighborhood,

        @NotBlank(message = "{address.city.required}")
        @Size(max = 100, message = "{address.city.size}")
        String city,

        @NotBlank(message = "{address.state.required}")
        @Size(max = 100, message = "{address.state.size}")
        String state
) {
}
