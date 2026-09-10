package io.github.jvlealc.marketsphere.customers.repository;

import io.github.jvlealc.marketsphere.customers.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressJpaRepository extends JpaRepository<Address, Long> {

    Optional<Address> findByCustomerId(Long customerId);

    Optional<Address> findByIdAndCustomerId(Long addressId, Long customerId);

    boolean existsByCustomerId(Long customerId);
}
