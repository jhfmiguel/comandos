package com.comandos.security.config;

import com.fariamiguel.app.core.config.PlatformProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConfigurationProperties(prefix = "erp")
public class ComandosPlatformProperties extends PlatformProperties {
}