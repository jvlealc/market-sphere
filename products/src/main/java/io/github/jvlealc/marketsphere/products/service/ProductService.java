package io.github.jvlealc.marketsphere.products.service;

import io.github.jvlealc.marketsphere.products.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.products.dto.ProductRequestDto;
import io.github.jvlealc.marketsphere.products.dto.ProductResponseDto;
import io.github.jvlealc.marketsphere.products.model.Product;
import io.github.jvlealc.marketsphere.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    @Transactional
    public ProductResponseDto createProduct(ProductRequestDto productRequestDto) {
        Product product = new Product(productRequestDto.name(), productRequestDto.unitPrice(), productRequestDto.description());
        repository.save(product);
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getUnitPrice(),
                product.getDescription(),
                product.isActive()
        );
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProductById(Long productId) {
        Product product = repository.findById(productId)
                .orElseThrow( () -> new ProductNotFoundException(productId) );
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getUnitPrice(),
                product.getDescription(),
                product.isActive()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return repository.findAll()
                .stream()
                .map( product -> new ProductResponseDto(
                        product.getId(),
                        product.getName(),
                        product.getUnitPrice(),
                        product.getDescription(),
                        product.isActive()
                ))
                .toList();
    }

    /**
     * Busca uma lista de produtos <strong>ativos</strong> e <strong>inativos</strong> por uma lista de IDs.
     * @param productsIds IDs dos produtos
     */
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByIdsIncludingInactive(List<Long> productIds) {
        return repository.findProductsByIdsIncludingInactive(productIds)
                .stream()
                .map(product -> new ProductResponseDto(
                        product.getId(),
                        product.getName(),
                        product.getUnitPrice(),
                        product.getDescription(),
                        product.isActive()
                ))
                .toList();
    }

    /**
     * Realiza a exclusão lógica de um produto
     * @param productId ID do produto a ser inativado
     * */
    @Transactional
    public void deleteProductById(Long productId) {
        if (!repository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        repository.deleteById(productId);
    }

    /**
     * Reativa um produto que foi logicamente excluído.
     * @param productId O ID do produto a ser reativado.
     */
    @Transactional
    public void reactivateProductById(Long productId) {
        Product productToReactivate = repository.findInactiveById(productId)
                .orElseThrow( () -> new ProductNotFoundException("Inactive product with ID " + productId + " not found.") );
        productToReactivate.setActive(true);
    }
}
