package com.comandos.inventory.service;

import com.comandos.inventory.model.AmmunitionBox;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AmmunitionBoxService {
    private final EntityManager em;
    private final AccessPolicy access;

    public AmmunitionBoxService(EntityManager em, AccessPolicy access) {
        this.em = em;
        this.access = access;
    }

    public record PageResult(List<Map<String, Object>> content, long totalElements, int page, int size) {}

    public PageResult list(int page, int size, Long organizationId, Long lotId, String intakeRequestId, String status) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination.");
        access.requireAny("inventory/lots", "READ");

        var clauses = new java.util.ArrayList<String>();
        var parameters = new LinkedHashMap<String, Object>();
        clauses.add(access.predicate("inventory/lots", "READ", "b",
            new AccessPolicy.Scope("location.organization.id", "location.unit.id")));
        if (organizationId != null) {
            clauses.add("b.location.organization.id = :organizationId");
            parameters.put("organizationId", organizationId);
        }
        if (lotId != null) {
            clauses.add("b.lot.id = :lotId");
            parameters.put("lotId", lotId);
        }
        if (intakeRequestId != null && !intakeRequestId.isBlank()) {
            clauses.add("b.intakeRequestId = :intakeRequestId");
            parameters.put("intakeRequestId", intakeRequestId.trim());
        }
        if (status != null && !status.isBlank()) {
            clauses.add("b.status = :status");
            parameters.put("status", status.trim().toUpperCase(java.util.Locale.ROOT));
        }
        String where = " where " + String.join(" and ", clauses);
        var query = em.createQuery("select b from AmmunitionBox b" + where + " order by b.lot.id, b.sequenceNumber", AmmunitionBox.class);
        var count = em.createQuery("select count(b) from AmmunitionBox b" + where, Long.class);
        parameters.forEach((name, value) -> {
            query.setParameter(name, value);
            count.setParameter(name, value);
        });
        return new PageResult(query.setFirstResult(page * size).setMaxResults(size).getResultList().stream().map(this::view).toList(),
            count.getSingleResult(), page, size);
    }

    public Map<String, Object> get(long id) {
        access.requireAny("inventory/lots", "READ");
        AmmunitionBox box = em.find(AmmunitionBox.class, id);
        if (box == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ammunition box not found.");
        access.requireScope("inventory/lots", "READ", box.location.organization.id,
            box.location.unit == null ? null : box.location.unit.id);
        return view(box);
    }

    private Map<String, Object> view(AmmunitionBox box) {
        var result = new LinkedHashMap<String, Object>();
        result.put("id", box.id);
        result.put("version", box.version);
        result.put("boxCode", box.boxCode);
        result.put("intakeRequestId", box.intakeRequestId);
        result.put("sequenceNumber", box.sequenceNumber);
        result.put("lotId", box.lot.id);
        result.put("lotNumber", box.lot.lotNumber);
        result.put("modelId", box.lot.model.id);
        result.put("modelName", box.lot.model.name);
        result.put("locationId", box.location.id);
        result.put("locationName", box.location.name);
        result.put("nominalQuantity", box.nominalQuantity);
        result.put("available", box.available);
        result.put("reserved", box.reserved);
        result.put("blocked", box.blocked);
        result.put("status", box.status);
        result.put("openedAt", box.openedAt);
        return result;
    }
}
