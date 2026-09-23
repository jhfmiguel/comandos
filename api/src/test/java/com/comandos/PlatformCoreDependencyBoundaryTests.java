package com.comandos;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlatformCoreDependencyBoundaryTests {

    private static final List<String> PLATFORM_PACKAGES = List.of(
        "core",
        "identity",
        "organization",
        "security",
        "audit",
        "workflow",
        "documents",
        "notifications",
        "messaging",
        "analytics"
    );

    private static final List<String> FORBIDDEN_DOMAIN_PREFIXES = List.of(
        "com.comandos.inventory",
        "com.comandos.purchase",
        "com.comandos.custody",
        "com.comandos.consumption",
        "com.comandos.donation",
        "com.comandos.transfer",
        "com.comandos.disposal",
        "com.comandos.maintenance",
        "com.comandos.reservation",
        "com.comandos.reconciliation",
        "com.comandos.sales",
        "com.comandos.lifecycle",
        "com.comandos.report",
        "com.comandos.transport",
        "com.comandos.escort",
        "com.comandos.intelligence",
        "com.comandos.operations",
        "com.comandos.personnel",
        "com.comandos.procurement"
    );

    @Test
    void platformCoreDoesNotDependOnVerticalDomains() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/comandos");

        for (String platformPackage : PLATFORM_PACKAGES) {
            Path module = sourceRoot.resolve(platformPackage);
            if (!Files.exists(module)) continue;

            try (var files = Files.walk(module)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);

                    for (String forbidden : FORBIDDEN_DOMAIN_PREFIXES) {
                        assertTrue(
                            !source.contains("import " + forbidden),
                            () -> file + " must not depend on vertical domain " + forbidden
                        );
                    }
                }
            }
        }
    }
}
