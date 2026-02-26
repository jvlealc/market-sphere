package io.github.jvlealc.marketsphere.orders.client.customers;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.config.CustomersClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customers",
        url = "${market-sphere.feign.clients.customers.base-url}",
        configuration = CustomersClientConfig.class
)
public interface CustomersClient {

    @GetMapping("/{customerId}")
    ResponseEntity<CustomerRepresentation> getCustomerById(@PathVariable Long customerId);

    @GetMapping("/for-orders-service/{customerId}")
    ResponseEntity<CustomerRepresentation> getCustomerByIdIgnoringFilter(@PathVariable Long customerId);
}
