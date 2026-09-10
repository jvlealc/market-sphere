package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.client.feign.customer;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customers",
        url = "${market-sphere.feign.clients.customers.base-url}"
)
public interface CustomerFeignClient {

    @GetMapping("/{customerId}")
    ResponseEntity<CustomerRepresentation> getCustomerById(@PathVariable Long customerId);
}
