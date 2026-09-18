package io.github.jvlealc.marketsphere.products.mapper;

import io.github.jvlealc.marketsphere.products.dto.ProductRequest;
import io.github.jvlealc.marketsphere.products.dto.ProductResponse;
import io.github.jvlealc.marketsphere.products.model.Product;
import io.github.jvlealc.marketsphere.products.shared.rest.pagination.PageModel;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ProductRestMapper {

    public Product toEntity(ProductRequest request) {
        Objects.requireNonNull(request, "request cannot be null");

        return new Product(
                request.name(),
                request.unitPrice(),
                request.description()
        );
    }

    public ProductResponse toResponse(Product entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return new ProductResponse(
                entity.getId(),
                entity.getName(),
                entity.getUnitPrice(),
                entity.getDescription(),
                entity.isActive()
        );
    }

    public PageModel<ProductResponse> toPageModel(Page<Product> page) {
        Objects.requireNonNull(page, "page cannot be null");

        List<ProductResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageModel<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
