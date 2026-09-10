package io.github.jvlealc.marketsphere.customers.service;

import io.github.jvlealc.marketsphere.customers.client.brasilapi.BrasilApiPostalCodeValidator;
import io.github.jvlealc.marketsphere.customers.dto.AddressRequest;
import io.github.jvlealc.marketsphere.customers.dto.AddressResponse;
import io.github.jvlealc.marketsphere.customers.exception.AddressNotFoundException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerAddressAlreadyExistsException;
import io.github.jvlealc.marketsphere.customers.exception.CustomerNotFoundException;
import io.github.jvlealc.marketsphere.customers.mapper.AddressRestMapper;
import io.github.jvlealc.marketsphere.customers.model.Address;
import io.github.jvlealc.marketsphere.customers.model.Customer;
import io.github.jvlealc.marketsphere.customers.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerAddressService {

    private final AddressPersistenceService addressPersistenceService;
    private final AddressRestMapper addressMapper;
    private final CustomerJpaRepository customerRepository;
    private final BrasilApiPostalCodeValidator brasilApiValidator;

    public Long createAddress(Long customerId, AddressRequest request) {
        Customer existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (addressPersistenceService.existsByCustomerId(customerId)) {
            throw new CustomerAddressAlreadyExistsException(customerId);
        }

        brasilApiValidator.validate(request.postalCode());

        Address address = addressMapper.toEntity(request);
        address.setCustomer(existingCustomer);

        return addressPersistenceService.create(address).getId();
    }

    public void upsertAddress(Long customerId, AddressRequest request) {
        Customer existingCustomer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        brasilApiValidator.validate(request.postalCode());

        Address address = addressMapper.toEntity(request);
        address.setCustomer(existingCustomer);

        addressPersistenceService.upsert(customerId, address);
    }

    public AddressResponse getAddress(Long customerId, Long addressId) {
        if (!customerRepository.existsActiveById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        return addressMapper.toResponse(addressPersistenceService.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new AddressNotFoundException(addressId, customerId))
        );
    }
}
