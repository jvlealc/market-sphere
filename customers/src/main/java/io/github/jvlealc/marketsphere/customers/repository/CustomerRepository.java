package io.github.jvlealc.marketsphere.customers.repository;

import io.github.jvlealc.marketsphere.customers.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    /**
     * Busca um cliente INATIVO pelo ID
     * Isso "quebra" o filtro global @SQLRestriction para casos de reativação.
     *
     * @param customerId ID do cliente inativo
     * @return {@code Optional<Product>} possível cliente inativo
     */
    @Query(value = "SELECT * FROM customers c WHERE c.id = :customerId AND c.active = false", nativeQuery = true)
    Optional<Customer> findInactiveById(@Param("customerId") Long customerId);

    /**
     * Busca um cliente pelo ID esteja ele <strong>ativo</strong> ou <strong>inativo</strong>.
     * Isso "quebra" o filtro global @SQLRestriction para servir dados ao microsserviço de Pedidos (Orders)
     *
     * @param customerId ID do cliente
     * @return {@code Optional<Customer>}  possível cliente
     */
    @Query(value = "SELECT * FROM customers c WHERE c.id = :customerId", nativeQuery = true)
    Optional<Customer> findByIdIgnoringFilter(@Param("customerId") Long customerId);
}
