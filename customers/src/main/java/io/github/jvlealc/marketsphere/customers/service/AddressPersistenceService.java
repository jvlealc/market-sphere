package io.github.jvlealc.marketsphere.customers.service;

import io.github.jvlealc.marketsphere.customers.model.Address;
import io.github.jvlealc.marketsphere.customers.repository.AddressJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class AddressPersistenceService {

    private final AddressJpaRepository addressRepository;

    @Transactional
    Address create(Address address) {
        return addressRepository.save(address);
    }

    @Transactional
    void upsert(Long customerId, Address newValues) {
        Address existingAddress = addressRepository.findByCustomerId(customerId)
                .orElse(null);

        if (existingAddress == null) {
            addressRepository.save(newValues);
            return;
        }

        existingAddress.setPostalCode(newValues.getPostalCode());
        existingAddress.setStreet(newValues.getStreet());
        existingAddress.setHouseNumber(newValues.getHouseNumber());
        existingAddress.setComplement(newValues.getComplement());
        existingAddress.setNeighborhood(newValues.getNeighborhood());
        existingAddress.setCity(newValues.getCity());
        existingAddress.setState(newValues.getState());
        existingAddress.setCountry(newValues.getCountry());
    }

    @Transactional(readOnly = true)
    boolean existsByCustomerId(Long customerId) {
        return addressRepository.existsByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    Optional<Address> findByIdAndCustomerId(Long addressId, Long customerId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId);
    }
}
