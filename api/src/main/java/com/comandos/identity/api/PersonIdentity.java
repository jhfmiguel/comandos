package com.comandos.identity.api;

import java.time.LocalDate;

@Deprecated(forRemoval = true)
public record PersonIdentity(
    long id,
    String personType,
    String fullName,
    String taxId,
    LocalDate birthDate,
    String phone,
    String email,
    boolean active
) {
}
