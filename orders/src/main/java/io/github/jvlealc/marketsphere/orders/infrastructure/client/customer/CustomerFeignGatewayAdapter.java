package io.github.jvlealc.marketsphere.orders.infrastructure.client.customer;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.application.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.ExternalServiceException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerFeignGatewayAdapter implements CustomerGatewayPort {

    private final CustomerFeignClient customerClient;

    @Override
    public CustomerProfile getCustomerById(Long customerId) {
        requireNonNull(customerId, "customerId must not be null");
        try {
            ResponseEntity<CustomerRepresentation> response = customerClient.getCustomerById(customerId);

            CustomerRepresentation representation = Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Default] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerNotFoundException("customerId", "Customer not found or returned an empty response for ID: " + customerId);
                    });

            return toCustomerProfile(representation);

        } catch (FeignException.NotFound e) {
            log.warn("[Default] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerNotFoundException("customerId", "Customer not found with ID: " + customerId);
        } catch (FeignException e) {
            log.error("[Default] Error while calling customer service. For customer ID: {}. Status: {}. Message: {}", customerId, e.status(), e.getMessage());
            throw new ExternalServiceException("Error while calling customer service. For customer ID: " + customerId, e);
        }
    }

    @Override
    public CustomerProfile getCustomerByIdIncludingInactive(Long customerId) {
        requireNonNull(customerId, "customerId must not be null");
        try {
            ResponseEntity<CustomerRepresentation> response = customerClient.getCustomerByIdIncludingInactives(customerId);

            CustomerRepresentation representation = Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Ignoring Inactive Filter] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerNotFoundException("customerId", "Customer not found or returned an empty response for ID: " + customerId);
                    });

            return toCustomerProfile(representation);

        } catch (FeignException.NotFound e) {
            log.warn("[Ignoring Inactive Filter] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerNotFoundException("customerId", "Customer not found with ID: " + customerId);
        } catch (FeignException e) {
            log.error("[Ignoring Inactive Filter] Error while calling customer service. For customer ID: {}. Status: {}. Message: {}", customerId, e.status(), e.getMessage());
            throw new ExternalServiceException("Error while calling customer service. For customer ID: " + customerId, e);
        }
    }

    private static CustomerProfile toCustomerProfile(CustomerRepresentation representation) {
        return new CustomerProfile(
                representation.id(),
                representation.fullName(),
                representation.nationalId(),
                representation.email(),
                representation.phoneNumber(),
                representation.postalCode(),
                representation.street(),
                representation.houseNumber(),
                representation.complement(),
                representation.neighborhood(),
                representation.city(),
                representation.state(),
                representation.country(),
                representation.active()
        );
    }
}
