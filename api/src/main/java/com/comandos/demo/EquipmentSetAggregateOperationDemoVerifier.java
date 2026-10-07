package com.comandos.demo;

import com.comandos.consumption.model.ConsumableUsage;
import com.comandos.disposal.model.DisposalProcess;
import com.comandos.donation.model.Donation;
import com.comandos.inventory.model.EquipmentSet;
import com.comandos.inventory.model.EquipmentSetComponent;
import com.comandos.inventory.model.EquipmentSetOperation;
import com.comandos.inventory.model.EquipmentSetOperationComponent;
import com.comandos.maintenance.model.WorkOrder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1995)
public class EquipmentSetAggregateOperationDemoVerifier implements ApplicationRunner {
    private final EntityManager em;

    public EquipmentSetAggregateOperationDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (var set : em.createQuery("select s from EquipmentSet s order by s.id", EquipmentSet.class).getResultList()) {
            verifySet(set);
        }
        for (var operation : em.createQuery("select o from EquipmentSetOperation o order by o.id", EquipmentSetOperation.class).getResultList()) {
            verifyOperation(operation);
        }
    }

    private void verifySet(EquipmentSet set) {
        require(set.organizationLegacyId != null, "Equipment set must belong to an organization.");
        require(notBlank(set.code) && notBlank(set.name), "Equipment set must preserve code and name.");
        var components = em.createQuery("select c from EquipmentSetComponent c where c.equipmentSet.id=:id order by c.id", EquipmentSetComponent.class)
            .setParameter("id", set.id).getResultList();
        Set<String> inventoryKeys = new HashSet<>();
        for (var component : components) {
            require((component.asset == null) != (component.balance == null), "Equipment set component must reference exactly one asset or balance.");
            require(component.quantity != null && component.quantity.signum() > 0, "Equipment set component quantity must be positive.");
            require(notBlank(component.role), "Equipment set component role is required.");
            if (component.asset != null) {
                require(component.quantity.compareTo(BigDecimal.ONE) == 0, "Serialized equipment set component quantity must be one.");
                require(java.util.Objects.equals(component.asset.location.organizationLegacyId, set.organizationLegacyId), "Equipment set asset must belong to the same organization.");
                require(inventoryKeys.add("A" + component.asset.id), "Equipment set cannot duplicate an asset.");
            } else {
                require(java.util.Objects.equals(component.balance.location.organizationLegacyId, set.organizationLegacyId), "Equipment set balance must belong to the same organization.");
                require(inventoryKeys.add("B" + component.balance.id), "Equipment set cannot duplicate a stock balance.");
            }
        }
    }

    private void verifyOperation(EquipmentSetOperation operation) {
        require(operation.equipmentSet != null && operation.organizationLegacyId != null, "Aggregate kit operation must preserve set and organization.");
        require(java.util.Objects.equals(operation.equipmentSet.organizationLegacyId, operation.organizationLegacyId), "Aggregate kit operation organization must match the set.");
        require(notBlank(operation.setCode) && notBlank(operation.setName), "Aggregate kit operation must preserve set snapshots.");
        require(Set.of("DONATION", "DISPOSAL", "CONSUMPTION", "MAINTENANCE").contains(operation.operationType), "Unsupported aggregate kit operation type.");
        require("COMPLETED".equals(operation.status), "Aggregate kit operation must be completed atomically.");
        require(operation.executedAt != null, "Aggregate kit operation timestamp is required.");
        require(notBlank(operation.requestId) && notBlank(operation.requestFingerprint), "Aggregate kit operation idempotency metadata is required.");
        UUID.fromString(operation.requestId);
        require(operation.requestFingerprint.length() == 64, "Aggregate kit operation fingerprint must contain 64 characters.");

        var snapshots = em.createQuery("select c from EquipmentSetOperationComponent c where c.operation.id=:id order by c.id", EquipmentSetOperationComponent.class)
            .setParameter("id", operation.id).getResultList();
        require(!snapshots.isEmpty(), "Aggregate kit operation must preserve component snapshots.");
        require(operation.componentCount == snapshots.size(), "Aggregate kit component count must match the preserved snapshots.");
        Set<Long> sourceIds = new HashSet<>();
        for (var snapshot : snapshots) {
            require(sourceIds.add(snapshot.sourceComponentId), "Aggregate kit operation cannot duplicate a source component snapshot.");
            require(Set.of("ASSET", "BALANCE").contains(snapshot.componentKind), "Aggregate kit component kind must be ASSET or BALANCE.");
            require(snapshot.stockRecordId != null && snapshot.stockRecordId > 0, "Aggregate kit component stock reference is required.");
            require(notBlank(snapshot.role), "Aggregate kit component role snapshot is required.");
            require(snapshot.quantity != null && snapshot.quantity.signum() > 0, "Aggregate kit component quantity snapshot must be positive.");
            if ("CONSUMPTION".equals(operation.operationType)) require("BALANCE".equals(snapshot.componentKind), "Aggregate kit consumption may snapshot only consumable balances.");
            if ("MAINTENANCE".equals(operation.operationType)) require("ASSET".equals(snapshot.componentKind), "Aggregate kit maintenance may snapshot only serialized assets.");
        }

        List<Long> targetIds = targetIds(operation.aggregateRecordIds);
        require(!targetIds.isEmpty(), "Aggregate kit operation must preserve generated target records.");
        if ("MAINTENANCE".equals(operation.operationType)) {
            require(targetIds.size() == snapshots.size(), "Aggregate kit maintenance must generate one work order per serialized component.");
            targetIds.forEach(id -> require(em.find(WorkOrder.class, id) != null, "Aggregate kit maintenance work order must exist."));
        } else {
            require(targetIds.size() == 1, "Donation, disposal and consumption kit operations must generate one aggregate header.");
            Long id = targetIds.getFirst();
            if ("DONATION".equals(operation.operationType)) require(em.find(Donation.class, id) != null, "Aggregate kit donation must exist.");
            if ("DISPOSAL".equals(operation.operationType)) require(em.find(DisposalProcess.class, id) != null, "Aggregate kit disposal must exist.");
            if ("CONSUMPTION".equals(operation.operationType)) require(em.find(ConsumableUsage.class, id) != null, "Aggregate kit consumption must exist.");
        }
    }

    private static List<Long> targetIds(String value) {
        if (!notBlank(value)) return List.of();
        return Arrays.stream(value.split(",")).map(Long::valueOf).toList();
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
