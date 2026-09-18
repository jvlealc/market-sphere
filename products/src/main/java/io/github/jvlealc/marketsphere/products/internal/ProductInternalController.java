package io.github.jvlealc.marketsphere.products.internal;

import io.github.jvlealc.marketsphere.products.dto.ProductResponse;
import io.github.jvlealc.marketsphere.products.mapper.ProductRestMapper;
import io.github.jvlealc.marketsphere.products.shared.rest.pagination.PageModel;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

/**
 * Expõe endpoints internos destinados à comunicação com o microservice de pedidos e
 * operações internas da corporação.
 * <p>
 * Esses endpoints não fazem parte da API pública de produtos.
 */
@RestController
@RequestMapping("internal/products")
@RequiredArgsConstructor
@Validated
class ProductInternalController {

    private final ProductInternalService productInternalService;
    private final ProductRestMapper productMapper;

    @GetMapping(value = "/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductResponse> getProduct(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        return ResponseEntity.ok(
                productMapper.toResponse(productInternalService.getProductById(productId))
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageModel<ProductResponse>> getAllProducts(
            @RequestParam(value = "pageNumber", defaultValue = "0")
            @PositiveOrZero(message = "{pagination.pageNumber.positiveOrZero}")
            int pageNumber
    ) {
        return ResponseEntity.ok(
                productMapper.toPageModel(productInternalService.getAllProducts(pageNumber))
        );
    }

    // Integração com o microservice de pedidos (orders)
    @GetMapping(value = "/including-inactives", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<ProductResponse>> getProductsByIdsIncludingInactives(
            @RequestParam(value = "productsIds")
            @Size(min = 1, max = 50, message = "{product.productsIds.size}")
            Collection<Long> productsIds
    ) {
        return ResponseEntity.ok(
                productInternalService.getProductsByIdsIncludingInactives(productsIds).stream()
                        .map(productMapper::toResponse)
                        .toList()
        );
    }
}
