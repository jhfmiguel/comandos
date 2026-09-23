package com.comandos.core.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformPageTests {

    @Test
    void pageCopiesContentAndCalculatesNavigation() {
        var source = new ArrayList<>(List.of("a", "b"));

        var page = new PlatformPage<>(source, 25, 1, 10);
        source.add("c");

        assertEquals(List.of("a", "b"), page.content());
        assertEquals(3, page.totalPages());
        assertTrue(page.hasPrevious());
        assertTrue(page.hasNext());
        assertThrows(
            UnsupportedOperationException.class,
            () -> page.content().add("x")
        );
    }

    @Test
    void emptyPageHasNoNavigation() {
        var page = new PlatformPage<String>(null, 0, 0, 20);

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalPages());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());
    }

    @Test
    void invalidPaginationMetadataIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new PlatformPage<>(List.of(), -1, 0, 20)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PlatformPage<>(List.of(), 0, -1, 20)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new PlatformPage<>(List.of(), 0, 0, 0)
        );
    }
}
