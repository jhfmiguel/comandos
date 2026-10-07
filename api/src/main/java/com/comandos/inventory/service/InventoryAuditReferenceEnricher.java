package com.comandos.inventory.service;

import com.comandos.audit.api.AuditReferenceEnricher;
import com.comandos.inventory.model.StockLocation;
import jakarta.persistence.EntityManager;
import java.util.function.BiConsumer;
import org.springframework.stereotype.Component;

@Component
public class InventoryAuditReferenceEnricher implements AuditReferenceEnricher {

    private final EntityManager em;

    public InventoryAuditReferenceEnricher(EntityManager em) {
        this.em = em;
    }

    @Override
    public void enrich(String kind, long targetId, BiConsumer<String, Long> addReference) {
        if (!"inventory/locations".equals(kind)) return;

        var location = em.find(StockLocation.class, targetId);
        if (location == null) return;

        addReference.accept("organization", location.organizationLegacyId);
        if (location.unitLegacyId != null) addReference.accept("unit", location.unitLegacyId);
    }
}
