package com.comandos.core.service;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SystemPlatformClockTests {
    @Test
    void returnsCurrentInstant() {
        var clock = new SystemPlatformClock();
        var before = Instant.now();

        var current = clock.now();

        var after = Instant.now();
        assertFalse(current.isBefore(before));
        assertFalse(current.isAfter(after));
    }
}
