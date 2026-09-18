package io.github.jvlealc.marketsphere.products.controller;

import io.github.jvlealc.marketsphere.products.mapper.ProductRestMapper;
import io.github.jvlealc.marketsphere.products.shared.rest.pagination.PageModel;
import io.github.jvlealc.marketsphere.products.service.ProductService;
import io.github.jvlealc.marketsphere.products.dto.ProductRequest;
import io.github.jvlealc.marketsphere.products.dto.ProductResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("products")
@RequiredArgsConstructor
@Validated
class ProductController {

    private final ProductService productService;
    private final ProductRestMapper productMapper;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Void> createProduct(@RequestBody @Valid ProductRequest request) {
        Long productId = productService.createProduct(productMapper.toEntity(request));

        return ResponseEntity
                .created(buildHeaderLocation(productId))
                .build();
    }

    @GetMapping(value = "/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ProductResponse> getProduct(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        return ResponseEntity.ok(
                productMapper.toResponse(productService.getProductById(productId))
        );
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<PageModel<ProductResponse>> getAllProducts(
            @RequestParam(value = "pageNumber", defaultValue = "0")
            @PositiveOrZero(message = "{pagination.pageNumber.positiveOrZero}")
            int pageNumber
    ) {
        return ResponseEntity.ok(
                productMapper.toPageModel(productService.getAllProducts(pageNumber))
        );
    }

    /**
     * Realiza a exclusão lógica de um produto
     * @param productId ID do produto a ser inativado
     * @return {@code HTTP Status 204 - No Content} se bem-sucedido
     * */
    @DeleteMapping("/{productId}")
    ResponseEntity<Void> deactivateProduct(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        productService.deactivateProductById(productId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um produto lógicamente excluído
     * @param productId ID do produto a ser reativado
     * */
    @PostMapping("/{productId}/reactivate")
    ResponseEntity<Void> reactivateProduct(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        productService.reactivateProductById(productId);

        return ResponseEntity.noContent().build();
    }

    private static URI buildHeaderLocation(Long productId) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{productId}")
                .buildAndExpand(productId)
                .toUri();
    }
}
