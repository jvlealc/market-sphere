package io.github.jvlealc.marketsphere.products.repository;

import io.github.jvlealc.marketsphere.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    /**
     * Busca um produto INATIVO pelo ID
     * Isso "quebra" o filtro global @SQLRestriction para casos de reativação.
     *
     * @param productId ID do produto inativo
     * @return {@code Optional<Product>} Produto inativo
     */
    @Query(
            value = "SELECT * FROM products p WHERE p.id = :productId AND p.active = false",
            nativeQuery = true
    )
    Optional<Product> findInactiveById(Long productId);
}
