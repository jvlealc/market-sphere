package io.github.jvlealc.marketsphere.orders.application.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class ProductIdsExtractor {

    private ProductIdsExtractor() {
    }

    public static <T> List<Long> extract(
            List<T> items,
            Function<T, Long> idExtractor
    ) {
        requireNonNull(idExtractor, "ID extractor must not be null");

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items must not be null or empty");
        }

        Set<Long> ids = new LinkedHashSet<>(items.size());

        for (T item : items) {
            if (item == null) throw new IllegalArgumentException("Items must not contain null values");
            Long id = idExtractor.apply(item);
            if (id == null) throw new IllegalArgumentException("Extracted IDs must not contain null values");
            ids.add(id);
        }

        return new ArrayList<>(ids);
    }
}