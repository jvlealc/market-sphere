package io.github.jvlealc.marketsphere.products.internal;

import io.github.jvlealc.marketsphere.products.model.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;

interface ProductInternalReadRepository extends Repository<Product, Long> {

    /**
     * Busca produtos por uma lista de IDs, ignorando o filtro @SQLRestriction.
     * Usado pelo microservice de pedidos (orders).
     *
     * @param productIds lista de IDs de produtos
     *
     * @return {@code List<Product>} lista de produtos
     */
    @Query(
            value = "SELECT * FROM products p WHERE p.id IN :productIds",
            nativeQuery = true
    )
    List<Product> findByIdsIncludingInactives(Collection<Long> productIds);
}
