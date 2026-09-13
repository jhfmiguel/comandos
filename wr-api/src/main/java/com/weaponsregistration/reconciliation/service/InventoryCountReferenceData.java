package com.weaponsregistration.reconciliation.service;

import com.weaponsregistration.reconciliation.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryCountReferenceData implements ApplicationRunner {
    private final EntityManager em;
    public InventoryCountReferenceData(EntityManager em) { this.em = em; }
    @Override @Transactional public void run(ApplicationArguments args) {
        status("OPEN", "Open", false, 10); status("COUNTED", "Counted", false, 20);
        status("APPROVED", "Approved", true, 30); status("CANCELLED", "Cancelled", true, 40);
        result("MATCH", "Match", 10); result("SHORTAGE", "Shortage", 20); result("SURPLUS", "Surplus", 30);
    }
    private void status(String code,String name,boolean terminal,int order){if(em.createQuery("select count(s) from InventoryCountStatusType s where s.code=:c",Long.class).setParameter("c",code).getSingleResult()==0){var s=new InventoryCountStatusType();s.code=code;s.name=name;s.terminal=terminal;s.displayOrder=order;s.systemProtected=true;em.persist(s);}}
    private void result(String code,String name,int order){if(em.createQuery("select count(s) from InventoryCountResultType s where s.code=:c",Long.class).setParameter("c",code).getSingleResult()==0){var s=new InventoryCountResultType();s.code=code;s.name=name;s.displayOrder=order;s.systemProtected=true;em.persist(s);}}
}
