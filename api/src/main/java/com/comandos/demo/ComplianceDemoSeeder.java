package com.comandos.demo;

import com.comandos.compliance.model.CompliancePolicy;
import com.comandos.core.model.Organization;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(60)
public class ComplianceDemoSeeder implements ApplicationRunner {
    private final EntityManager em;

    public ComplianceDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization organization = em.createQuery("select o from Organization o where o.acronym='SSP-DEMO'", Organization.class)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
        if (organization == null) return;
        Long count = em.createQuery("select count(p) from CompliancePolicy p where p.organization.id=:org and p.unit is null", Long.class)
            .setParameter("org", organization.id).getSingleResult();
        if (count > 0) return;
        CompliancePolicy policy = new CompliancePolicy();
        policy.organization = organization;
        policy.expirationWarningDays = 30;
        policy.maintenanceWarningDays = 30;
        policy.regulatoryWarningDays = 30;
        policy.inspectionIntervalDays = 180;
        policy.active = true;
        em.persist(policy);
    }
}
