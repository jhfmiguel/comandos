package com.comandos.demo;

import com.comandos.core.model.Organization;
import com.comandos.core.model.OrganizationalUnit;
import com.comandos.disposal.model.Destruction;
import com.comandos.disposal.model.DisposalItem;
import com.comandos.disposal.model.DisposalProcess;
import com.comandos.inventory.model.AssetItem;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockMovement;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.inventory.model.StockMovementReferenceType;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(64)
public class DisposalLifecycleDemoSeeder implements ApplicationRunner {
    private final EntityManager em;

    public DisposalLifecycleDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (count(DisposalProcess.class) > 0) return;

        Organization org = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit unit = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");
        AssetItem asset = one(AssetItem.class, "assetCode", "PAT-DEMO-0004");
        StockLocation location = one(StockLocation.class, "code", "UOP-COFRE-01");
        if (org == null || unit == null || asset == null || location == null) return;

        LocalDateTime finalizedAt = LocalDateTime.now().minusDays(90);
        DisposalProcess process = new DisposalProcess();
        process.organization = org;
        process.unit = unit;
        process.organizationName = org.name;
        process.unitName = unit.name;
        process.processNumber = "PROC-DEMO-DESFAZIMENTO-001";
        process.reason = "Bem fictício considerado antieconômico para demonstrar baixa lógica terminal e destruição física.";
        process.status = "FINALIZED";
        process.finalizedAt = finalizedAt;
        process.finalizedById = 1L;
        process.finalizedByLogin = "maria.armeira.demo";
        process.requestId = id("disposal");
        process.requestFingerprint = fingerprint("disposal");
        em.persist(process);

        StockMovement movement = new StockMovement();
        movement.asset = asset;
        movement.location = location;
        movement.nature = StockMovementNature.DISPOSAL.name();
        movement.referenceType = StockMovementReferenceType.DISPOSAL.name();
        movement.referenceId = process.id;
        movement.quantity = BigDecimal.ONE.negate();
        movement.movedAt = finalizedAt;
        movement.operatorId = 1L;
        movement.operatorLogin = "maria.armeira.demo";
        movement.notes = "Baixa terminal fictícia para homologação de desfazimento.";
        em.persist(movement);

        DisposalItem item = new DisposalItem();
        item.process = process;
        item.model = asset.model;
        item.asset = asset;
        item.location = location;
        item.movement = movement;
        item.modelName = asset.model.name;
        item.sku = asset.model.sku;
        item.stockCode = asset.assetCode;
        item.locationName = location.name;
        item.unitOfMeasure = asset.model.unitOfMeasure;
        item.quantity = BigDecimal.ONE;
        em.persist(item);

        asset.status = AssetStatus.DISPOSED.name();

        Destruction destruction = new Destruction();
        destruction.process = process;
        destruction.method = "Inutilização controlada - exemplo didático";
        destruction.destroyedAt = finalizedAt;
        destruction.certificate = "CERT-DEMO-DESTRUICAO-001";
        em.persist(destruction);
        em.flush();
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + "=:value", type)
            .setParameter("value", value)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private long count(Class<?> type) {
        return em.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult();
    }

    private String id(String key) {
        return UUID.nameUUIDFromBytes(("comandos-demo-" + key).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String fingerprint(String key) {
        return String.format("%064x", Math.abs(key.hashCode()) + 3000L);
    }
}
