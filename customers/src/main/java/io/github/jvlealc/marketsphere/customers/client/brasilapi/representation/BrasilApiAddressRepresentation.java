package io.github.jvlealc.marketsphere.customers.client.brasilapi.representation;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiAddressRepresentation(

        @JsonAlias("cep")
        String postalCode,
        String state,
        String city,
        String neighborhood,
        String street
) { }
