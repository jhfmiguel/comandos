package com.comandos.core.api;

import java.util.List;

/**
 * @deprecated Use {@link com.fariamiguel.core.api.PlatformPage}. This record is a
 * temporary source-compatible adapter for COMANDOS product code.
 */
@Deprecated(forRemoval = true)
public record PlatformPage<T>(
    List<T> content,
    long totalElements,
    int page,
    int size
) {
    public PlatformPage {
        content = content == null ? List.of() : List.copyOf(content);
        if (totalElements < 0) throw new IllegalArgumentException("totalElements cannot be negative.");
        if (page < 0) throw new IllegalArgumentException("page cannot be negative.");
        if (size <= 0) throw new IllegalArgumentException("size must be greater than zero.");
    }

    public PlatformPage(com.fariamiguel.core.api.PlatformPage<T> shared) {
        this(shared.content(), shared.totalElements(), shared.page(), shared.size());
    }

    public com.fariamiguel.core.api.PlatformPage<T> toShared() {
        return new com.fariamiguel.core.api.PlatformPage<>(content, totalElements, page, size);
    }

    public int totalPages() {
        return toShared().totalPages();
    }

    public boolean hasNext() {
        return toShared().hasNext();
    }

    public boolean hasPrevious() {
        return toShared().hasPrevious();
    }
}
