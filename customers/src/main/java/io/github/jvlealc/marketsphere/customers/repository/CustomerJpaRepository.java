package io.github.jvlealc.marketsphere.customers.repository;

import io.github.jvlealc.marketsphere.customers.model.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerJpaRepository extends JpaRepository<Customer, Long> {

    @EntityGraph(attributePaths = "address")
    @Override
    Optional<Customer> findById(Long customerId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customers c WHERE c.email = :email)", nativeQuery = true)
    boolean existsByEmailIncludingInactive(String email);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customers c WHERE c.national_id = :nationalId)", nativeQuery = true)
    boolean existsByNationalIdIncludingInactive(String nationalId);

    /**
     * Busca um cliente INATIVO pelo ID
     * Isso "quebra" o filtro global @SQLRestriction para casos de reativação.
     *
     * @param customerId ID do cliente inativo
     * @return {@code Optional<Product>} possível cliente inativo
     */
    @Query(value = "SELECT * FROM customers c WHERE c.id = :customerId AND c.active = false", nativeQuery = true)
    Optional<Customer> findInactiveById(@Param("customerId") Long customerId);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM customers c WHERE c.id = :customerId AND c.active = true)", nativeQuery = true)
    boolean existsActiveById(Long customerId);
}
