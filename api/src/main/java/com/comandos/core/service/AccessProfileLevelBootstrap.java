package com.comandos.core.service;

import com.comandos.core.model.AccessProfile;
import com.comandos.core.model.AccessProfileLevel;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AccessProfileLevelBootstrap implements ApplicationRunner {

    private final EntityManager entityManager;

    public AccessProfileLevelBootstrap(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AccessProfileLevel system = ensure(
            "SYSTEM",
            "Sistema",
            "Acesso com escopo global, abrangendo todas as organizações."
        );
        AccessProfileLevel organization = ensure(
            "ORGANIZATION",
            "Organização",
            "Acesso limitado à organização vinculada ao usuário."
        );
        AccessProfileLevel unit = ensure(
            "UNIT",
            "Unidade",
            "Acesso limitado à unidade organizacional vinculada ao usuário."
        );

        var profiles = entityManager.createQuery(
                "select p from AccessProfile p where p.levelType is null",
                AccessProfile.class)
            .getResultList();

        for (AccessProfile profile : profiles) {
            String legacy = profile.level == null
                ? ""
                : profile.level.trim().toUpperCase(Locale.ROOT);

            profile.levelType = switch (legacy) {
                case "SYSTEM" -> system;
                case "ORGANIZATION" -> organization;
                case "UNIT" -> unit;
                default -> legacy.isBlank()
                    ? null
                    : ensure(
                        legacy,
                        profile.level.trim(),
                        "Migrado automaticamente do nível legado"
                    );
            };
        }
    }

    private AccessProfileLevel ensure(String code, String name, String description) {
        var existing = entityManager.createQuery(
                "select l from AccessProfileLevel l where upper(l.code) = :code",
                AccessProfileLevel.class)
            .setParameter("code", code.toUpperCase(Locale.ROOT))
            .setMaxResults(1)
            .getResultList();

        if (!existing.isEmpty()) return existing.getFirst();

        AccessProfileLevel value = new AccessProfileLevel();
        value.code = code.toUpperCase(Locale.ROOT);
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
        return value;
    }
}
