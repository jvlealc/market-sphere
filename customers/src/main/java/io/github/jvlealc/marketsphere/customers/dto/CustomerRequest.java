package io.github.jvlealc.marketsphere.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "{customer.fullName.required}")
        @Size(max = 200, message = "{customer.fullName.size}")
        String fullName,

        @NotBlank(message = "{customer.nationalId.required}")
        @Pattern(regexp = "\\d{11}", message = "{customer.nationalId.invalid}")
        String nationalId,

        @NotBlank(message = "{customer.email.required}")
        @Email(message = "{customer.email.invalid}")
        @Size(max = 150, message = "{customer.email.size}")
        String email,

        @NotBlank(message = "{customer.phoneNumber.required}")
        @Size(max = 25, message = "{customer.phoneNumber.size}")
        String phoneNumber
) {
}
