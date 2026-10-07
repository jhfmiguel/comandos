package com.comandos.demo;

import com.comandos.disposal.model.Destruction;
import com.comandos.disposal.model.DisposalItem;
import com.comandos.disposal.model.DisposalProcess;
import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.inventory.model.StockMovementReferenceType;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1990)
public class DisposalLifecycleDemoVerifier implements ApplicationRunner {
    private final EntityManager em;

    public DisposalLifecycleDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var processes = em.createQuery("select p from DisposalProcess p order by p.id", DisposalProcess.class).getResultList();
        require(!processes.isEmpty(), "Disposal homologation requires at least one finalized process.");
        Set<String> requestIds = new HashSet<>();
        Set<String> processKeys = new HashSet<>();
        for (DisposalProcess process : processes) verifyProcess(process, requestIds, processKeys);
    }

    private void verifyProcess(DisposalProcess process, Set<String> requestIds, Set<String> processKeys) {
        require(process.organizationLegacyId != null, "Disposal must preserve organization provenance.");
        require(notBlank(process.organizationName), "Disposal must preserve organization snapshot.");
        require(notBlank(process.processNumber), "Disposal process number is required.");
        require(notBlank(process.reason), "Disposal reason is required.");
        require("FINALIZED".equals(process.status), "Disposal process must be terminal FINALIZED.");
        require(process.finalizedAt != null, "Disposal finalization timestamp is required.");
        require(process.finalizedById != null && notBlank(process.finalizedByLogin), "Disposal must preserve finalizer identity.");
        require(notBlank(process.requestId) && notBlank(process.requestFingerprint), "Disposal must preserve idempotency metadata.");
        UUID.fromString(process.requestId);
        require(process.requestFingerprint.length() == 64, "Disposal request fingerprint must use the canonical 64-character hash.");
        require(requestIds.add(process.requestId), "Disposal request ID must be globally unique.");
        require(processKeys.add(process.organizationLegacyId + "|" + process.processNumber), "Disposal process number must be unique inside the organization.");

        var items = em.createQuery("select i from DisposalItem i where i.process.id=:id order by i.id", DisposalItem.class)
            .setParameter("id", process.id).getResultList();
        require(!items.isEmpty() && items.size() <= 100, "Finalized disposal must contain 1 to 100 items.");
        Set<String> stockSelections = new HashSet<>();
        for (DisposalItem item : items) verifyItem(process, item, stockSelections);

        var destruction = em.createQuery("select d from Destruction d where d.process.id=:id", Destruction.class)
            .setParameter("id", process.id).getResultList();
        require(destruction.size() <= 1, "Disposal process can have at most one physical destruction record.");
        if (!destruction.isEmpty()) verifyDestruction(process, destruction.getFirst());
    }

    private void verifyItem(DisposalProcess process, DisposalItem item, Set<String> stockSelections) {
        require(item.process != null && item.process.id.equals(process.id), "Disposal item must belong to its process.");
        require(item.model != null && item.location != null && item.movement != null, "Disposal item must preserve model, location and movement provenance.");
        require((item.asset == null) != (item.lot == null), "Disposal item must reference exactly one asset or lot.");
        require(item.quantity != null && item.quantity.signum() > 0, "Disposal item quantity must be positive.");
        require(notBlank(item.modelName) && notBlank(item.sku) && notBlank(item.stockCode) && notBlank(item.locationName) && notBlank(item.unitOfMeasure),
            "Disposal item must preserve inventory snapshots.");

        String selection = item.asset != null ? "A" + item.asset.id : "L" + item.lot.id + "@" + item.location.id;
        require(stockSelections.add(selection), "Disposal cannot duplicate the same inventory selection in one process.");

        var movement = item.movement;
        require(StockMovementNature.DISPOSAL.name().equals(movement.nature), "Disposal item requires DISPOSAL stock movement.");
        require(StockMovementReferenceType.DISPOSAL.name().equals(movement.referenceType), "Disposal movement must preserve DISPOSAL reference type.");
        require(process.id.equals(movement.referenceId), "Disposal movement must reference the finalized process.");
        require(movement.location != null && movement.location.id.equals(item.location.id), "Disposal movement must preserve the item location.");
        require(movement.movedAt != null && movement.movedAt.equals(process.finalizedAt), "Disposal movement timestamp must match logical finalization.");
        require(movement.quantity != null && movement.quantity.compareTo(item.quantity.negate()) == 0, "Disposal movement must be the exact negative item quantity.");
        require(movement.operatorId != null && notBlank(movement.operatorLogin), "Disposal movement must preserve operator identity.");

        if (item.asset != null) {
            require(item.quantity.compareTo(BigDecimal.ONE) == 0, "Serialized disposal requires quantity one.");
            require(movement.asset != null && movement.asset.id.equals(item.asset.id) && movement.lot == null, "Serialized disposal movement must reference only the asset.");
            require(AssetStatus.DISPOSED.name().equals(item.asset.status), "Disposed asset must remain in terminal DISPOSED state.");
        } else {
            require(movement.lot != null && movement.lot.id.equals(item.lot.id) && movement.asset == null, "Lot disposal movement must reference only the lot.");
            require(item.lot.availableQuantity != null && item.lot.availableQuantity.signum() >= 0, "Lot disposal cannot leave negative aggregate availability.");
        }
    }

    private void verifyDestruction(DisposalProcess process, Destruction destruction) {
        require(destruction.process != null && destruction.process.id.equals(process.id), "Physical destruction must belong to its logical disposal process.");
        require(notBlank(destruction.method), "Physical destruction method is required.");
        require(destruction.destroyedAt != null, "Physical destruction timestamp is required.");
        require(!destruction.destroyedAt.isAfter(LocalDateTime.now()), "Physical destruction cannot be dated in the future.");
        require(notBlank(destruction.certificate), "Physical destruction certificate is required.");
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
