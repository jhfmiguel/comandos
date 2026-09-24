package com.comandos.core.service;

import com.comandos.core.model.Person;
import com.comandos.core.model.PersonType;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class PersonTypeBootstrap implements ApplicationRunner {

    private final EntityManager entityManager;

    public PersonTypeBootstrap(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        PersonType individual = ensure(
            "INDIVIDUAL",
            "Pessoa física",
            "Pessoa natural / física"
        );
        PersonType legalEntity = ensure(
            "LEGAL_ENTITY",
            "Pessoa jurídica",
            "Pessoa jurídica / organização"
        );

        var people = entityManager.createQuery(
                "select p from Person p where p.personTypeRef is null",
                Person.class)
            .getResultList();

        for (Person person : people) {
            String legacy = person.personType == null
                ? ""
                : person.personType.trim().toUpperCase(Locale.ROOT);

            if ("INDIVIDUAL".equals(legacy)) {
                person.personTypeRef = individual;
            } else if ("LEGAL_ENTITY".equals(legacy)) {
                person.personTypeRef = legalEntity;
            } else if (!legacy.isBlank()) {
                person.personTypeRef = ensure(
                    legacy,
                    person.personType.trim(),
                    "Migrado automaticamente do tipo legado"
                );
            }
        }
    }

    private PersonType ensure(String code, String name, String description) {
        var existing = entityManager.createQuery(
                "select t from PersonType t where t.code = :code",
                PersonType.class)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();

        if (!existing.isEmpty()) return existing.getFirst();

        PersonType value = new PersonType();
        value.code = code;
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
        return value;
    }
}
