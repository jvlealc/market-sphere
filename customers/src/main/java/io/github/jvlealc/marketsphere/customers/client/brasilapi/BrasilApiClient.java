package io.github.jvlealc.marketsphere.customers.client.brasilapi;

import io.github.jvlealc.marketsphere.customers.client.brasilapi.config.BrasilApiFeignConfig;
import io.github.jvlealc.marketsphere.customers.client.brasilapi.representation.BrasilApiAddressRepresentation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "brasilapi",
        url = "${market-sphere.feign.clients.brasilapi.base-url}",
        configuration = BrasilApiFeignConfig.class
)
public interface BrasilApiClient {

    @GetMapping(
            value = "/cep/v1/{brazilianPostalCode}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    BrasilApiAddressRepresentation getAddressByPostalCode(@PathVariable String brazilianPostalCode);
}
