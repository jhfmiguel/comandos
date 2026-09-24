package com.comandos.core.service;

import com.comandos.core.model.OrganizationalUnit;
import com.comandos.core.model.OrganizationalUnitType;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class OrganizationalUnitTypeBootstrap implements ApplicationRunner {

    private final EntityManager entityManager;

    public OrganizationalUnitTypeBootstrap(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var units = entityManager.createQuery(
                "select u from OrganizationalUnit u where u.unitType is null",
                OrganizationalUnit.class)
            .getResultList();

        for (OrganizationalUnit unit : units) {
            String raw = unit.type == null ? "" : unit.type.trim();
            if (raw.isBlank()) continue;

            unit.unitType = ensure(raw);
        }
    }

    private OrganizationalUnitType ensure(String raw) {
        String code = raw
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");

        if (code.isBlank()) code = "LEGACY_UNIT_TYPE";

        var existing = entityManager.createQuery(
                "select t from OrganizationalUnitType t where upper(t.code) = :code or lower(t.name) = lower(:name)",
                OrganizationalUnitType.class)
            .setParameter("code", code)
            .setParameter("name", raw)
            .setMaxResults(1)
            .getResultList();

        if (!existing.isEmpty()) return existing.getFirst();

        OrganizationalUnitType value = new OrganizationalUnitType();
        value.code = code;
        value.name = raw;
        value.description = "Migrado automaticamente do tipo legado da unidade organizacional";
        value.active = true;
        entityManager.persist(value);
        return value;
    }
}
