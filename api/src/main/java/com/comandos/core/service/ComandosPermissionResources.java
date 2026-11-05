package com.comandos.core.service;
import com.fariamiguel.app.core.service.PermissionResourceContributor;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ComandosPermissionResources implements PermissionResourceContributor {
    @Override
    public Set<String> resources() { return Set.of("custodies", "ammunition-consumptions"); }
}