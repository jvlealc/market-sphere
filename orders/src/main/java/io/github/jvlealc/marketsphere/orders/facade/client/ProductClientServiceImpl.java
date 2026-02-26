package io.github.jvlealc.marketsphere.orders.facade.client;

import feign.FeignException;
import io.github.jvlealc.marketsphere.orders.client.products.ProductsClient;
import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.client.products.ProductClientNotFoundException;
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
public class ProductClientServiceImpl implements ProductClientService {

    private final ProductsClient productsClient;

    @Override
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

    @Override
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
