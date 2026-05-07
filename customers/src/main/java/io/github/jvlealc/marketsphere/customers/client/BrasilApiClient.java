package io.github.jvlealc.marketsphere.customers.client;

import io.github.jvlealc.marketsphere.customers.config.ClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "brasilapi",
        url = "${market-sphere.feign.clients.brasilapi.base-url}",
        configuration = ClientConfig.class
)
public interface BrasilApiClient {

    @GetMapping(
            value = "/cep/v1/{brazilianPostalCode}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    BrasilApiAddressRepresentation getAddressByPostalCode(@PathVariable String brazilianPostalCode);
}
