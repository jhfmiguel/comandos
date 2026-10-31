/**
 * Compatibility overlay for legacy COMANDOS enterprise callers.
 *
 * <p>The canonical Enterprise Core is owned by {@code jhfmiguel/faria-miguel} and
 * consumed through {@code com.fariamiguel.enterprise.*}. No new generic people,
 * organization, contact, customer, supplier, procurement, sales, finance,
 * contracts, catalog or inventory behavior may be implemented in this package.
 * The remaining catalog subtree exists only until product callers are migrated.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Enterprise Compatibility Overlay")
package com.comandos.enterprise;
