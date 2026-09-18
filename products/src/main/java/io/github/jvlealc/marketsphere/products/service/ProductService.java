package io.github.jvlealc.marketsphere.products.service;

import io.github.jvlealc.marketsphere.products.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.products.model.Product;
import io.github.jvlealc.marketsphere.products.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int PAGE_SIZE = 20;

    private final ProductJpaRepository productRepository;

    @Transactional
    public Long createProduct(Product product) {
        Product saved = productRepository.save(product);

        return saved.getId();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional(readOnly = true)
    public Page<Product> getAllProducts(int pageNumber) {
        return productRepository.findAll(PageRequest.of(
                pageNumber,
                PAGE_SIZE,
                Sort.by(DESC, "updatedAt").and(Sort.by(ASC, "id"))
        ));
    }

    /**
     * Realiza a exclusão lógica de um produto pelo seu ID
     * */
    @Transactional
    public void deactivateProductById(Long productId) {
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        productRepository.delete(existingProduct);
    }

    /**
     * Reativa um produto que foi logicamente excluído pelo seu ID.
     */
    @Transactional
    public void reactivateProductById(Long productId) {
        Product existingProduct = productRepository.findInactiveById(productId)
                .orElseThrow(() -> new ProductNotFoundException("No inactive product found with ID " + productId));

        existingProduct.setActive(true);
    }
}
