package io.github.jvlealc.marketsphere.products.service;

import io.github.jvlealc.marketsphere.products.dto.ProductRequestDto;
import io.github.jvlealc.marketsphere.products.dto.ProductResponseDto;

import java.util.List;

public interface ProductService {

    ProductResponseDto createProduct(ProductRequestDto productRequestDto);

    ProductResponseDto getProductById(Long productId);

    List<ProductResponseDto> getAllProducts();

    /**
     * Busca uma lista de produtos <strong>ativos</strong> e <strong>inativos</strong> por uma lista de IDs.
     * @param productsIds IDs dos produtos
     */
    List<ProductResponseDto> getAllProductsByIdsIgnoringFilter(List<Long> productsIds);

    /**
     * Realiza a exclusão lógica de um produto
     * @param productId ID do produto a ser inativado
     * */
    void deleteProductById(Long productId);

    /**
     * Reativa um produto que foi logicamente excluído.
     * @param productId O ID do produto a ser reativado.
     */
    void reactivateProductById(Long productId);
}
