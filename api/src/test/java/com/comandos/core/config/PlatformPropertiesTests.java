package com.comandos.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PlatformPropertiesTests {

    @Test
    void bindsReusablePlatformProperties() {
        var source = new MapConfigurationPropertySource(Map.of(
            "platform.allowed-origin", "https://example.test",
            "platform.security.require-login", "true",
            "platform.security.enforce-permissions", "true"
        ));

        var properties = new Binder(source)
            .bind("platform", Bindable.of(PlatformProperties.class))
            .orElseThrow(() -> new IllegalStateException("Platform properties were not bound."));

        assertEquals("https://example.test", properties.getAllowedOrigin());
        assertTrue(properties.getSecurity().isRequireLogin());
        assertTrue(properties.getSecurity().isEnforcePermissions());
    }

    @Test
    void keepsSafeDefaultsWhenOptionalPropertiesAreAbsent() {
        var properties = new PlatformProperties();

        assertEquals("http://localhost:3000", properties.getAllowedOrigin());
        assertFalse(properties.getSecurity().isRequireLogin());
        assertFalse(properties.getSecurity().isEnforcePermissions());
    }
}
