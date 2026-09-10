package io.github.jvlealc.marketsphere.customers.client.brasilapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrasilApiAddressRepresentation(
        @JsonProperty("cep")
        String postalCode
) {
}
