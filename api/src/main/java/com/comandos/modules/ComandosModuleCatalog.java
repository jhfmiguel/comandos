package com.comandos.modules;

import com.fariamiguel.modules.api.ModuleCatalogContributor;
import com.fariamiguel.modules.api.ModuleCapability;
import com.fariamiguel.modules.api.ModuleDefinition;
import com.fariamiguel.modules.api.ModuleDependency;
import com.fariamiguel.modules.api.ModuleId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Owns the COMANDOS-specific module definitions; the shared catalog stays product-neutral. */
@Component
public final class ComandosModuleCatalog implements ModuleCatalogContributor {
    @Override
    public List<ModuleDefinition> modules() {
        return List.of(
            module("comandos.armament", "Armamento",
                deps("enterprise.inventory", "enterprise.procurement", "enterprise.personnel"),
                caps("armament.assets", "armament.ammunition")),
            module("comandos.transport", "Transporte",
                deps("enterprise.inventory", "enterprise.personnel"),
                caps("transport.fleet", "transport.dispatch")),
            module("comandos.escort", "Escolta",
                deps("enterprise.personnel", "comandos.transport"),
                caps("escort.planning", "escort.execution")),
            module("comandos.intelligence", "Inteligência",
                deps("enterprise.personnel"), caps("intelligence.casework")),
            module("comandos.operations", "Operações",
                deps("enterprise.personnel", "comandos.transport"),
                caps("operations.planning", "operations.execution"))
        );
    }

    private static ModuleDefinition module(
            String id,
            String name,
            Set<ModuleDependency> dependencies,
            Set<ModuleCapability> capabilities) {
        return new ModuleDefinition(
            ModuleId.of(id), "comandos", name, "1", dependencies, capabilities, true, true
        );
    }

    private static Set<ModuleDependency> deps(String... ids) {
        return Arrays.stream(ids)
            .map(ModuleDependency::required)
            .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<ModuleCapability> caps(String... ids) {
        return Arrays.stream(ids)
            .map(id -> ModuleCapability.of(id, id))
            .collect(Collectors.toUnmodifiableSet());
    }
}