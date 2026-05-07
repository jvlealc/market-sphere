package io.github.jvlealc.marketsphere.orders.client.products;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductClientService {

    private final ProductsClient productsClient;

    /**
     * Busca um produto pelo ID. Garante que ele existe.
     * Lança ProductClientNotFoundException se não for encontrado (404 ou 200+null).
     *
     * @param productId O ID do produto a ser buscado.
     * @return A representação do produto.
     */
    public ProductRepresentation getProductById(Long productId) {
        try {
            ResponseEntity<ProductRepresentation> response = productsClient.getProductById(productId);

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> {
                        log.error("Product service returned a null body (200 OK) for productId: {}.", productId);
                        return new ProductClientNotFoundException("productId", "Product not found or returned an empty response for ID: " + productId);
                    });
        } catch (FeignException.NotFound e) {
            log.error("Product not found (404) via Feign client for productId: {}. Message: {}", productId, e.getMessage());
            throw new ProductClientNotFoundException("productId", "Product not found with ID: " + productId);
        }
    }

    /**
     * Busca uma lista de produtos por IDs.
     * Retorna um Mapa<ID, Produto> de forma segura.
     * Trata 200 OK + body nulo, retornando um mapa vazio se nenhum produto for encontrado.
     *
     * @param productIds A lista de IDs de produtos a serem buscados.
     * @return Um Mapa onde a chave é o ID do produto e o valor é a representação.
     */
    public Map<Long, ProductRepresentation> getProductsByIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        ResponseEntity<List<ProductRepresentation>> response = productsClient.getAllProductsByIds(productIds);

        List<ProductRepresentation> productRepresentations = Optional.ofNullable(response.getBody())
                .orElse(Collections.emptyList());

        return productRepresentations.stream()
                .collect(Collectors.toMap(
                        ProductRepresentation::id,
                        product -> product
                ));
    }
}
