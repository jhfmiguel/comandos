package com.comandos.demo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.lang.reflect.Modifier;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(200)
public class DemoCoverageVerifier implements ApplicationRunner {

    private final EntityManager entityManager;

    public DemoCoverageVerifier(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> empty = entityManager.getMetamodel().getEntities().stream()
            .map(EntityType::getJavaType)
            .filter(type -> type.getPackageName().startsWith("com.comandos"))
            .filter(type -> !Modifier.isAbstract(type.getModifiers()))
            .filter(type -> count(type) == 0)
            .map(Class::getSimpleName)
            .sorted()
            .toList();

        if (!empty.isEmpty()) {
            throw new IllegalStateException(
                "Demo database still has empty mapped tables: " + String.join(", ", empty)
            );
        }
    }

    private long count(Class<?> type) {
        String entityName = entityManager.getMetamodel().entity(type).getName();
        return entityManager
            .createQuery("select count(e) from " + entityName + " e", Long.class)
            .getSingleResult();
    }
}
