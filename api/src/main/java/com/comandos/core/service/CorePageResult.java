package com.comandos.core.service;

import java.util.List;
import java.util.Map;

/**
 * Stable HTTP pagination shape for Core catalog resources.
 *
 * <p>Kept outside CoreService so canonical read paths do not depend on the
 * legacy monolithic service during migration.</p>
 */
public record CorePageResult(
    List<Map<String, Object>> content,
    long totalElements,
    int page,
    int size
) {
    public CorePageResult {
        content = List.copyOf(content);
    }
}
