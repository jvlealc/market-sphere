package io.github.jvlealc.marketsphere.customers.client.brasilapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "brasilapi",
        url = "${market-sphere.feign.clients.brasilapi.base-url}"
)
public interface BrasilApiFeignClient {

    @GetMapping(
            value = "/cep/v2/{brazilianPostalCode}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<BrasilApiAddressRepresentation> getAddressByPostalCode(@PathVariable String brazilianPostalCode);
}
