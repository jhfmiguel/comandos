package com.comandos.identity.api;

import java.util.Optional;

public interface IdentityDirectory {
    Optional<PersonIdentity> findPerson(long id);
}
