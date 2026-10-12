package com.comandos.core.service;

import com.comandos.core.model.CoreEntity;

/**
 * Canonical master-data write contract for COMANDOS core workflows.
 *
 * Core application services must not depend on migration infrastructure.
 * The runtime adapter is replaceable once canonical write ownership is
 * transferred to the Faria Miguel enterprise persistence layer.
 */
public interface CanonicalMasterDataWritePort {

    /**
     * Persists a canonical representation of a stored business entity.
     * Returns whether this domain entity has a canonical representation.
     */
    boolean synchronize(CoreEntity entity);

    /**
     * Applies a business deletion to the corresponding canonical entity.
     */
    boolean delete(CoreEntity entity);
}
