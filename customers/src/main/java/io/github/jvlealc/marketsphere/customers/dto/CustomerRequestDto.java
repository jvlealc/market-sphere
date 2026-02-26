package io.github.jvlealc.marketsphere.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequestDto(

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
        String phoneNumber,

        @NotBlank(message = "{customer.postalCode.required}")
        @Pattern(regexp = "\\d{5}-?\\d{3}", message = "{customer.postalCode.invalid}")
        String postalCode,

        @NotBlank(message = "{customer.number.required}")
        @Size(max = 10, message = "{customer.number.size}")
        String number,

        @Size(max = 50, message = "{customer.complement.size}")
        String complement,

        @NotBlank(message = "{customer.country.required}")
        @Size(max = 100, message = "{customer.country.size}")
        String country
) { }
