package com.comandos.identity.service;

import com.comandos.core.model.Person;
import com.comandos.identity.api.IdentityDirectory;
import com.comandos.identity.api.PersonIdentity;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class JpaIdentityDirectory implements IdentityDirectory {

    private final EntityManager entityManager;

    public JpaIdentityDirectory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<PersonIdentity> findPerson(long id) {
        Person person = entityManager.find(Person.class, id);

        if (person == null) return Optional.empty();

        return Optional.of(new PersonIdentity(
            person.id,
            person.personType,
            person.fullName,
            person.taxId,
            person.birthDate,
            person.phone,
            person.email,
            Boolean.TRUE.equals(person.active)
        ));
    }
}
