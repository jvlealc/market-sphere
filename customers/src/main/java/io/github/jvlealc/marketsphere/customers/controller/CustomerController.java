package io.github.jvlealc.marketsphere.customers.controller;

import io.github.jvlealc.marketsphere.customers.dto.AddressRequest;
import io.github.jvlealc.marketsphere.customers.dto.AddressResponse;
import io.github.jvlealc.marketsphere.customers.dto.CustomerRequest;
import io.github.jvlealc.marketsphere.customers.dto.CustomerResponse;
import io.github.jvlealc.marketsphere.customers.service.CustomerAddressService;
import io.github.jvlealc.marketsphere.customers.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("customers")
@RequiredArgsConstructor
@Validated
class CustomerController {

    private final CustomerService customerService;
    private final CustomerAddressService addressService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE )
    ResponseEntity<Void> createCustomer(@RequestBody @Valid CustomerRequest request) {
        Long customerId = customerService.createCustomer(request);

        return ResponseEntity
                .created(buildHeaderLocation(customerId, "/{customerId}"))
                .build();
    }

    @GetMapping(value = "/{customerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId
    ) {
        return ResponseEntity.ok(customerService.getCustomerById(customerId));
    }

    @PutMapping(value = "/{customerId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> updateCustomer(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId,
            @RequestBody @Valid CustomerRequest request
    ) {
        customerService.updateCustomer(customerId, request);

        return ResponseEntity.noContent().build();
    }

    /**
     * Realiza a exclusão lógica de um cliente
     *
     * @param customerId ID do cliente a ser inativado
     *
     * @return {@code HTTP Status 204 - No Content} se bem-sucedido
     * */
    @DeleteMapping("/{customerId}")
    ResponseEntity<Void> deactivateCustomerById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId
    ) {
        customerService.deactivateCustomerById(customerId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um cliente lógicamente excluído
     *
     * @param customerId ID do cliente a ser reativado
     *
     * @return {@code HTTP Status 204 - No Content} se bem-sucedido
     * */
    @PostMapping("/{customerId}/reactivate")
    ResponseEntity<Void> reactivateCustomerById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId
    ) {
        customerService.reactivateCustomerById(customerId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{customerId}/addresses", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createAddress(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId,
            @RequestBody @Valid AddressRequest request
    ) {
        Long addressId = addressService.createAddress(customerId, request);

        return ResponseEntity
                .created(buildHeaderLocation(addressId, "/{addressId}"))
                .build();
    }

    @GetMapping(value = "/{customerId}/addresses/{addressId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AddressResponse> getAddressById(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId,
            @PathVariable @Positive(message = "{address.id.positive}") Long addressId
    ) {
        return ResponseEntity.ok(addressService.getAddress(customerId, addressId));
    }

    @PutMapping(value = "/{customerId}/addresses", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> upsertAddress(
            @PathVariable @Positive(message = "{customer.id.positive}") Long customerId,
            @RequestBody @Valid AddressRequest request
    ) {
        addressService.upsertAddress(customerId, request);

        return ResponseEntity.noContent().build();
    }

    private static URI buildHeaderLocation(Object resourceId, String path) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path(path)
                .buildAndExpand(resourceId)
                .toUri();
    }
}
