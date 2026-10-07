package com.comandos.demo;

import com.comandos.consumption.model.*;
import com.comandos.inventory.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1980)
public class ConsumableUsageLifecycleDemoVerifier implements ApplicationRunner {
    private final EntityManager em;
    public ConsumableUsageLifecycleDemoVerifier(EntityManager em) { this.em = em; }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var eligibleFamilies = em.createQuery(
            "select distinct c.family from ItemCategory c where c.consumable=true and c.lotControlled=true and c.serialized=false order by c.family",
            String.class).getResultList();
        require(eligibleFamilies.contains("AMMUNITION"), "Ammunition must remain eligible for consumable usage.");
        require(eligibleFamilies.stream().anyMatch(f -> !"AMMUNITION".equals(f)),
            "At least one non-ammunition family must support the generic consumable lifecycle.");

        var usages = em.createQuery("select u from ConsumableUsage u order by u.id", ConsumableUsage.class).getResultList();
        for (var usage : usages) {
            require(usage.organizationLegacyId != null
                    && usage.responsibleLegacyId != null
                    && usage.authorizerLegacyId != null,
                "Consumable usage must preserve organization, responsible person and authorizer.");
            require("CLOSED".equals(usage.status), "Consumable usage must be closed after reconciliation.");
            require(usage.deliveredAt != null && usage.closedAt != null && !usage.closedAt.isBefore(usage.deliveredAt),
                "Consumable usage lifecycle timestamps are inconsistent.");

            var items = em.createQuery("select i from ConsumableUsageItem i where i.usage.id=:id order by i.id", ConsumableUsageItem.class)
                .setParameter("id", usage.id).getResultList();
            require(!items.isEmpty(), "Consumable usage must have at least one item.");
            for (var item : items) verify(item, usage.id);
        }
    }

    private void verify(ConsumableUsageItem item, Long usageId) {
        require(item.lot != null && item.balance != null && item.location != null, "Consumable usage item must preserve stock provenance.");
        var category = item.lot.model.category;
        require(Boolean.TRUE.equals(category.consumable) && Boolean.TRUE.equals(category.lotControlled)
                && !Boolean.TRUE.equals(category.serialized),
            "Consumable usage item must belong to a non-serialized lot-controlled consumable family.");
        require(Objects.equals(item.family, category.family), "Consumable family snapshot is inconsistent.");
        require(item.deliveredQuantity != null && item.usedQuantity != null && item.returnedQuantity != null,
            "Delivered, used and returned quantities are required.");
        require(item.deliveredQuantity.signum() > 0 && item.usedQuantity.signum() >= 0 && item.returnedQuantity.signum() >= 0,
            "Consumable lifecycle quantities cannot be negative.");
        require(item.usedQuantity.add(item.returnedQuantity).compareTo(item.deliveredQuantity) == 0,
            "Delivered quantity must equal used plus returned quantity.");
        require(item.deliveryMovement != null
                && StockMovementNature.CONSUMABLE_DELIVERY.name().equals(item.deliveryMovement.nature)
                && item.deliveryMovement.quantity.compareTo(item.deliveredQuantity.negate()) == 0,
            "Delivery must have an equivalent negative movement.");
        require(StockMovementReferenceType.CONSUMABLE_USAGE.name().equals(item.deliveryMovement.referenceType)
                && Objects.equals(item.deliveryMovement.referenceId, usageId),
            "Delivery movement must point to its consumable usage.");
        if (item.returnedQuantity.signum() > 0) {
            require(item.returnMovement != null
                    && StockMovementNature.CONSUMABLE_RETURN.name().equals(item.returnMovement.nature)
                    && item.returnMovement.quantity.compareTo(item.returnedQuantity) == 0,
                "Returned surplus must have an equivalent positive movement.");
            require(item.returnedAt != null, "Returned surplus requires a return timestamp.");
        } else {
            require(item.returnMovement == null, "Zero returned quantity cannot have a return movement.");
        }
        BigDecimal netMovement = item.deliveryMovement.quantity.add(item.returnMovement == null ? BigDecimal.ZERO : item.returnMovement.quantity);
        require(netMovement.compareTo(item.usedQuantity.negate()) == 0,
            "Delivery plus return movements must equal the net used quantity.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Consumable usage regression failed: " + message);
    }
}
