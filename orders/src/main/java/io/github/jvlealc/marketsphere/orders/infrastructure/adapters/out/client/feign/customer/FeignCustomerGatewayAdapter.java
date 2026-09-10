package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.client.feign.customer;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.application.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.ExternalServiceException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.model.customer.CustomerAddress;
import io.github.jvlealc.marketsphere.orders.application.model.customer.CustomerProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeignCustomerGatewayAdapter implements CustomerGatewayPort {

    private final CustomerFeignClient customerClient;

    @Override
    public CustomerProfile getCustomerById(Long customerId) {
        Objects.requireNonNull(customerId, "customerId must not be null");
        try {
            ResponseEntity<CustomerRepresentation> response = customerClient.getCustomerById(customerId);

            CustomerRepresentation representation = Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("[Default] Customer service returned a null body (200 OK) for customerId: {}.", customerId);
                        return new CustomerNotFoundException("Customer not found or returned an empty response for ID: " + customerId);
                    });

            return toCustomerProfile(representation);

        } catch (FeignException.NotFound e) {
            log.warn("[Default] Customer not found (404) via Feign client for customer ID: {}. Message: {}", customerId, e.getMessage());
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        } catch (FeignException e) {
            log.error("[Default] Error while calling customer service. For customer ID: {}. Status: {}. Message: {}", customerId, e.status(), e.getMessage());
            throw new ExternalServiceException("Error while calling customer service. For customer ID: " + customerId, e);
        }
    }

    private static CustomerProfile toCustomerProfile(CustomerRepresentation representation) {
        AddressRepresentation address = representation.address();

        return new CustomerProfile(
                representation.id(),
                representation.fullName(),
                representation.nationalId(),
                representation.email(),
                representation.phoneNumber(),
                address != null ? toCustomerAddress(address) : null,
                representation.active()
        );
    }

    private static CustomerAddress toCustomerAddress(AddressRepresentation address) {
        return new CustomerAddress(
                address.postalCode(),
                address.street(),
                address.houseNumber(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state(),
                address.country()
        );
    }
}
