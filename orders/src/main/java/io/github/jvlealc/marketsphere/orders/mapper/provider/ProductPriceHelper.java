package io.github.jvlealc.marketsphere.orders.mapper.provider;

import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.ProductUnitPriceUnavailableException;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Context;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductPriceHelper {

    @Named("fetchProductUnitPrice")
    public BigDecimal fetchProductUnitPrice(Long productId, @Context Map<Long, ProductRepresentation> productsMap) {
        // Validação de entrada (Fail-Fast)
        if (productId == null || productsMap == null) {
            return BigDecimal.ZERO;
        }

        ProductRepresentation product =  productsMap.get(productId);

        if (product == null) {
            throw new ProductUnitPriceUnavailableException("Product ID " + productId + " missing in context map.");
        }

        if (product.unitPrice() ==  null) {
            throw new ProductUnitPriceUnavailableException("Product ID " + productId + " has null price.");
        }

        return product.unitPrice();
    }
}
