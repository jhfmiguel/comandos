package com.comandos.identity.api;

import java.util.Optional;

@Deprecated(forRemoval = true)
public interface IdentityDirectory {
    Optional<PersonIdentity> findPerson(long id);
}
