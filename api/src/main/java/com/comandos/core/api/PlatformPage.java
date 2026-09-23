package com.comandos.core.api;

import java.util.List;

public record PlatformPage<T>(
    List<T> content,
    long totalElements,
    int page,
    int size
) {
    public PlatformPage {
        content = content == null ? List.of() : List.copyOf(content);
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements cannot be negative.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero.");
        }
    }

    public int totalPages() {
        if (totalElements == 0) return 0;
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0 && totalPages() > 0;
    }
}
