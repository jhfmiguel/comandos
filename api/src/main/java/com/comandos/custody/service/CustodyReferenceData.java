package com.comandos.custody.service;

import com.comandos.custody.model.CustodyReturnConditionType;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CustodyReferenceData implements ApplicationRunner {
    private final EntityManager em;
    public CustodyReferenceData(EntityManager em) { this.em = em; }
    @Override @Transactional public void run(ApplicationArguments args) {
        seed("GOOD", "Good", false, 10);
        seed("NEEDS_INSPECTION", "Needs inspection", true, 20);
        seed("DAMAGED", "Damaged", true, 30);
    }
    private void seed(String code, String name, boolean blocks, int order) {
        if (em.createQuery("select count(t) from CustodyReturnConditionType t where t.code = :code", Long.class)
                .setParameter("code", code).getSingleResult() > 0) return;
        var type = new CustodyReturnConditionType(); type.code = code; type.name = name;
        type.blocksAvailability = blocks; type.displayOrder = order; type.systemProtected = true; em.persist(type);
    }
}
