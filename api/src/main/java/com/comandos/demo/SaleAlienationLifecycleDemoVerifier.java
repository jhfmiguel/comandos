package com.comandos.demo;

import com.comandos.inventory.model.AssetStatus;
import com.comandos.inventory.model.StockMovementNature;
import com.comandos.sales.model.InventorySale;
import com.comandos.sales.model.InventorySaleItem;
import com.comandos.sales.model.SaleReturnItem;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1975)
public class SaleAlienationLifecycleDemoVerifier implements ApplicationRunner {
    private final EntityManager em;

    public SaleAlienationLifecycleDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<InventorySale> sales = em.createQuery("select s from InventorySale s order by s.id", InventorySale.class).getResultList();
        require(!sales.isEmpty(), "Sale regression requires at least one finalized sale.");
        for (InventorySale sale : sales) verifySale(sale);
    }

    private void verifySale(InventorySale sale) {
        require(notBlank(sale.processNumber), "Finalized sale requires process number.");
        require(notBlank(sale.legalBasis), "Finalized sale requires legal basis.");
        require(notBlank(sale.documentReference), "Finalized sale requires document reference.");
        require(sale.finalizedAt != null, "Sale requires finalized timestamp.");
        require(sale.withdrawnAt != null, "Sale requires definitive withdrawal timestamp.");
        require(notBlank(sale.withdrawnByLogin), "Sale requires withdrawal operator.");
        require("WITHDRAWN".equals(sale.withdrawalState), "Finalized sale must be definitively withdrawn.");
        require("TRANSFERRED_TO_BUYER".equals(sale.titleTransferState), "Finalized sale must transfer title to buyer.");
        require(sale.titleTransferredAt != null, "Sale requires title transfer timestamp.");

        List<InventorySaleItem> items = em.createQuery(
            "select i from InventorySaleItem i where i.sale.id=:id order by i.id", InventorySaleItem.class)
            .setParameter("id", sale.id).getResultList();
        require(!items.isEmpty(), "Sale requires items.");

        BigDecimal calculatedTotal = BigDecimal.ZERO;
        for (InventorySaleItem item : items) {
            require((item.asset == null) != (item.lot == null), "Sale item must reference exactly one asset or lot.");
            require(item.quantity != null && item.quantity.signum() > 0, "Sale item quantity must be positive.");
            require(item.unitPrice != null && item.unitPrice.signum() >= 0, "Sale item price is required.");
            require(item.subtotal != null && item.subtotal.compareTo(item.unitPrice.multiply(item.quantity)) == 0,
                "Sale item subtotal must match quantity times unit price.");
            require("ORGANIZATION".equals(item.previousOwnerType) && notBlank(item.previousOwnerName),
                "Sale item must preserve institutional previous owner.");
            require("BUYER".equals(item.newOwnerType) && notBlank(item.newOwnerName),
                "Sale item must preserve buyer as new owner.");
            require(item.movement != null && StockMovementNature.SALE.name().equals(item.movement.nature),
                "Sale item requires SALE movement.");
            require(item.movement.quantity.signum() < 0, "Sale movement must remove inventory.");
            require(item.movement.referenceId != null && item.movement.referenceId.equals(sale.id),
                "Sale movement must reference its sale.");

            BigDecimal returned = returned(item.id);
            require(returned.signum() >= 0 && returned.compareTo(item.quantity) <= 0,
                "Returned quantity cannot exceed sold quantity.");
            if (item.asset != null) {
                if (returned.signum() == 0) {
                    require(AssetStatus.SOLD.name().equals(item.asset.status),
                        "Non-returned sold asset must remain definitively SOLD.");
                } else {
                    require(!AssetStatus.SOLD.name().equals(item.asset.status),
                        "Returned sold asset must leave SOLD state.");
                }
            }
            calculatedTotal = calculatedTotal.add(item.subtotal);
        }
        require(sale.total != null && sale.total.compareTo(calculatedTotal) == 0,
            "Sale total must equal item subtotals.");
    }

    private BigDecimal returned(Long itemId) {
        BigDecimal value = em.createQuery(
            "select sum(i.quantity) from SaleReturnItem i where i.saleItem.id=:id", BigDecimal.class)
            .setParameter("id", itemId).getSingleResult();
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
