package io.github.jvlealc.marketsphere.products.controller;

import io.github.jvlealc.marketsphere.products.controller.util.HeaderLocationBuilder;
import io.github.jvlealc.marketsphere.products.dto.ProductRequestDto;
import io.github.jvlealc.marketsphere.products.dto.ProductResponseDto;
import io.github.jvlealc.marketsphere.products.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated; // Import necessário
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService service;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> createProduct(@RequestBody @Valid ProductRequestDto productRequestDto) {
        ProductResponseDto productResponseDto = service.createProduct(productRequestDto);
        return ResponseEntity
                .created(HeaderLocationBuilder.build(productResponseDto.id()))
                .build();
    }

    @GetMapping(value = "/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponseDto> getProductById(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        return ResponseEntity.ok(service.getProductById(productId));
    }

    // Métod unificado para buscar todos os produtos ou por uma lista de IDs
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProductResponseDto>> getProducts(
            @RequestParam(value = "productsIds", required = false) List<Long> productsIds
    ) {
        if (productsIds != null && !productsIds.isEmpty()) {
            return ResponseEntity.ok(service.getAllProductsByIdsIgnoringFilter(productsIds));
        }
        return ResponseEntity.ok(service.getAllProducts());
    }

    /**
     * Realiza a exclusão lógica de um produto
     * @param productId ID do produto a ser inativado
     * @return {@code HTTP Status 204 - No Content} se bem-sucedido
     * */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProductById(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        service.deleteProductById(productId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um produto lógicamente excluído
     * @param productId ID do produto a ser reativado
     * */
    @PostMapping("/{productId}/reactivate")
    public ResponseEntity<Void> reactivateProductById(
            @PathVariable @Positive(message = "{product.id.positive}") Long productId
    ) {
        service.reactivateProductById(productId);
        return ResponseEntity.noContent().build();
    }
}
