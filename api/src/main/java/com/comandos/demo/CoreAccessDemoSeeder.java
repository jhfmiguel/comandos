package com.comandos.demo;

import com.comandos.core.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(35)
public class CoreAccessDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public CoreAccessDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Person person = one(Person.class, "taxId", "22222222222");
        Organization org = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit unit = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");
        AccessProfileLevel level = first(AccessProfileLevel.class);
        PermissionResource resource = first(PermissionResource.class);
        PermissionAction action = first(PermissionAction.class);
        if (person == null || org == null || unit == null) return;

        AccessProfile profile = first(AccessProfile.class);
        if (profile == null && level != null) {
            profile = new AccessProfile();
            profile.name = "Gestor de Armamento - demonstração";
            profile.levelType = level;
            profile.level = level.code;
            em.persist(profile);
        }

        Permission permission = first(Permission.class);
        if (permission == null && resource != null && action != null) {
            permission = new Permission();
            permission.resourceType = resource;
            permission.actionType = action;
            permission.resource = resource.code;
            permission.action = action.code;
            em.persist(permission);
        }

        if (profile != null && permission != null && count(ProfilePermission.class) == 0) {
            ProfilePermission link = new ProfilePermission();
            link.profile = profile;
            link.permission = permission;
            em.persist(link);
        }

        SystemUser user = first(SystemUser.class);
        if (user == null) {
            user = new SystemUser();
            user.person = person;
            user.login = "maria.armeira.demo";
            user.passwordHash = "{noop}Teste@123";
            user.mfaEnabled = false;
            user.blocked = false;
            em.persist(user);
        }

        if (profile != null && count(UserProfile.class) == 0) {
            UserProfile link = new UserProfile();
            link.user = user;
            link.profile = profile;
            link.organization = org;
            link.unit = unit;
            em.persist(link);
        }

        if (count(PersonCredential.class) == 0) {
            PersonCredential credential = new PersonCredential();
            credential.person = person;
            credential.type = "IDENTIFICAÇÃO_FUNCIONAL";
            credential.number = "IF-DEMO-0001";
            credential.validUntil = LocalDate.now().plusYears(5);
            em.persist(credential);
        }

        if (count(PersonQualification.class) == 0) {
            PersonQualification qualification = new PersonQualification();
            qualification.person = person;
            qualification.category = "Gestão e controle de armamento - treinamento";
            qualification.validUntil = LocalDate.now().plusYears(2);
            qualification.status = "ACTIVE";
            em.persist(qualification);
        }

        em.flush();
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private <T> T first(Class<T> type) {
        return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private long count(Class<?> type) {
        return em.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult();
    }
}
