package io.github.jvlealc.marketsphere.products.shared.rest.pagination;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public record PageModel<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements
) {
    public PageModel {
        Objects.requireNonNull(content, "content must not be null");

        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber cannot be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements cannot be negative");
        }

        if (content.size() > pageSize) {
            throw new IllegalArgumentException("content size cannot be greater than pageSize");
        }

        if (content.size() > totalElements) {
            throw new IllegalArgumentException("content size cannot be greater than totalElements");
        }

        content = List.copyOf(content);
    }

    @JsonProperty
    public int totalPages() {
        if (totalElements == 0) {
            return 0;
        }

        return (int) (
                totalElements / pageSize + (totalElements % pageSize == 0 ? 0 : 1)
        );
    }

    @JsonProperty
    public boolean hasNext() {
        return (pageNumber + 1) < totalPages();
    }

    @JsonProperty
    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    @JsonProperty
    public boolean isEmpty() {
        return content.isEmpty();
    }
}
