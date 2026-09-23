package com.comandos.core.service;

import com.comandos.core.api.PlatformClock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SystemPlatformClock implements PlatformClock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
