package io.github.jvlealc.marketsphere.customers.internal;

import io.github.jvlealc.marketsphere.customers.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface CustomerInternalReadRepository extends Repository<Customer, Long> {

    @EntityGraph(attributePaths = "address")
    Page<Customer> findAll(Pageable pageable);

    @Query(value = "SELECT * FROM customers c WHERE c.id = :customerId", nativeQuery = true)
    Optional<Customer> findByIdIncludingInactive(@Param("customerId") Long customerId);
}
