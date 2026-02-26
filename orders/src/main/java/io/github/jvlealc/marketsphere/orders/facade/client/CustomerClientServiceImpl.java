package io.github.jvlealc.marketsphere.orders.facade.client;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.client.customers.CustomersClient;
import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.client.customers.CustomerClientNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerClientServiceImpl implements CustomerClientService {

    private final CustomersClient customersClient;

    @Override
    public CustomerRepresentation getCustomerById(Long customerId) {
        try {
            ResponseEntity<CustomerRepresentation> response = customersClient.getCustomerById(customerId);
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Default] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerClientNotFoundException("customerId", "Customer not found or returned an empty response for ID: " + customerId);
                    });
        } catch (FeignException.NotFound e) {
            log.error("[Default] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerClientNotFoundException("customerId", "Customer not found with ID: " + customerId);
        }
    }

    @Override
    public CustomerRepresentation getCustomerByIdIgnoringFilter(Long customerId) {
        try {
            ResponseEntity<CustomerRepresentation> response = customersClient.getCustomerByIdIgnoringFilter(customerId);
            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Ignoring Filter] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerClientNotFoundException("customerId", "Customer not found or returned an empty response for ID: " + customerId);
                    });
        } catch (FeignException.NotFound e) {
            log.error("[Ignoring Filter] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerClientNotFoundException("customerId", "Customer not found with ID: " + customerId);
        }
    }
}
