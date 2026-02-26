package io.github.jvlealc.marketsphere.products.repository;

import io.github.jvlealc.marketsphere.products.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

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
    Optional<Product> findInactiveById(@Param("productId") Long productId);

    /**
     * Busca produtos por uma lista de IDs, ignorando o filtro @SQLRestriction
     * Usado por outros microsserviços
     *
     * @param productIds lista de IDs de produtos
     * @return {@code List<Product>} lista de produtos
     */
    @Query(value = "SELECT * FROM products p WHERE p.id IN :productIds", nativeQuery = true)
    List<Product> findProductsByIdsIgnoringFilter(@Param("productIds") List<Long> productIds);
}
