package com.comandos.core.service;

import com.comandos.core.model.Permission;
import com.comandos.core.model.PermissionAction;
import com.comandos.core.model.PermissionResource;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class PermissionParameterBootstrap implements ApplicationRunner {

    private final EntityManager entityManager;

    public PermissionParameterBootstrap(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String code : defaultResources()) {
            ensureResource(code, displayName(code), description(code));
        }

        ensureAction("READ", "Leitura", "Consultar registros do recurso.");
        ensureAction("CREATE", "Criar", "Criar novos registros do recurso.");
        ensureAction("UPDATE", "Alterar", "Alterar registros existentes do recurso.");
        ensureAction("DELETE", "Excluir", "Excluir registros do recurso.");
        ensureAction("MANAGE", "Administrar", "Administrar acesso e configurações sensíveis do recurso.");
        ensureAction("COUNT", "Contar", "Registrar a contagem física dos itens de inventário.");
        ensureAction("APPROVE", "Aprovar", "Aprovar ajustes apurados no inventário físico.");
        ensureAction("CANCEL", "Cancelar", "Cancelar uma contagem de inventário físico em andamento.");
        ensureAction("*", "Todas as ações", "Concede todas as ações suportadas pelo recurso.");

        var permissions = entityManager.createQuery(
                "select p from Permission p where p.resourceType is null or p.actionType is null",
                Permission.class)
            .getResultList();

        for (Permission permission : permissions) {
            if (permission.resourceType == null && permission.resource != null && !permission.resource.isBlank()) {
                permission.resourceType = ensureResource(
                    permission.resource.trim(),
                    displayName(permission.resource.trim()),
                    "Migrado automaticamente de permissão existente."
                );
            }
            if (permission.actionType == null && permission.action != null && !permission.action.isBlank()) {
                String code = permission.action.trim().toUpperCase(Locale.ROOT);
                permission.actionType = ensureAction(
                    code,
                    code,
                    "Migrado automaticamente de permissão existente."
                );
            }
        }
    }

    private Set<String> defaultResources() {
        Set<String> values = new LinkedHashSet<>();
        values.add("*");
        values.add("security/access");
        values.add("core/organizations");
        values.add("core/units");
        values.add("core/people");
        values.add("inventory/assets");
        values.add("inventory/locations");
        values.add("inventory/lots");
        values.add("sales");
        values.add("custodies");
        values.add("ammunition-consumptions");
        values.add("donations");
        values.add("transfers");
        values.add("disposals");
        values.add("maintenance");
        values.add("reservations");
        values.add("inventory-counts");
        return values;
    }

    private PermissionResource ensureResource(String code, String name, String description) {
        var existing = entityManager.createQuery(
                "select r from PermissionResource r where r.code = :code",
                PermissionResource.class)
            .setParameter("code", code)
            .setMaxResults(1)
            .getResultList();

        if (!existing.isEmpty()) return existing.getFirst();

        PermissionResource value = new PermissionResource();
        value.code = code;
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private PermissionAction ensureAction(String code, String name, String description) {
        var existing = entityManager.createQuery(
                "select a from PermissionAction a where upper(a.code) = :code",
                PermissionAction.class)
            .setParameter("code", code.toUpperCase(Locale.ROOT))
            .setMaxResults(1)
            .getResultList();

        if (!existing.isEmpty()) return existing.getFirst();

        PermissionAction value = new PermissionAction();
        value.code = code.toUpperCase(Locale.ROOT);
        value.name = name;
        value.description = description;
        value.active = true;
        entityManager.persist(value);
        return value;
    }

    private String displayName(String code) {
        if ("*".equals(code)) return "Todos os recursos";
        return code;
    }

    private String description(String code) {
        if ("*".equals(code)) return "Concede acesso a todos os recursos.";
        if ("security/access".equals(code)) return "Administração de usuários, perfis e permissões.";
        return "Recurso de segurança " + code + ".";
    }
}
