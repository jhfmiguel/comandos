package com.comandos.core.api;

import java.time.Instant;

public interface PlatformClock {
    Instant now();
}
