package io.github.jvlealc.marketsphere.products.internal;

import io.github.jvlealc.marketsphere.products.model.Product;
import io.github.jvlealc.marketsphere.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
class ProductInternalService {

    private final ProductService productService;
    private final ProductInternalReadRepository productRepository;

    Product getProductById(Long productId) {
        return productService.getProductById(productId);
    }

    Page<Product> getAllProducts(int pageNumber) {
        return productService.getAllProducts(pageNumber);
    }

    /**
     * Busca uma lista de produtos <strong>ativos</strong> e <strong>inativos</strong> por uma lista de IDs.
     */
    @Transactional(readOnly = true)
    List<Product> getProductsByIdsIncludingInactives(Collection<Long> productsIds) {
        Objects.requireNonNull(productsIds,  "productsIds must not be null");

        return productRepository.findByIdsIncludingInactives(new LinkedHashSet<>(productsIds));
    }
}
