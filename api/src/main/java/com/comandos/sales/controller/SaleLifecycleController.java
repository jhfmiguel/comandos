package com.comandos.sales.controller;

import com.comandos.inventory.model.AssetStatus;
import com.comandos.sales.model.InventorySale;
import com.comandos.sales.model.InventorySaleItem;
import com.comandos.sales.model.SaleReturn;
import com.comandos.sales.model.SaleReturnItem;
import com.comandos.security.service.AccessPolicy;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/erp/sales")
public class SaleLifecycleController {
    private final EntityManager em;
    private final AccessPolicy access;

    public SaleLifecycleController(EntityManager em, AccessPolicy access) {
        this.em = em;
        this.access = access;
    }

    @GetMapping("/{id}/lifecycle")
    public LifecycleView lifecycle(@PathVariable long id) {
        InventorySale sale = em.find(InventorySale.class, id);
        if (sale == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found.");
        access.requireEntity("sales", "READ", sale);
        List<InventorySaleItem> items = em.createQuery(
            "select i from InventorySaleItem i where i.sale.id=:id order by i.id", InventorySaleItem.class)
            .setParameter("id", id).getResultList();
        List<SaleReturn> returns = em.createQuery(
            "select r from SaleReturn r where r.sale.id=:id order by r.id", SaleReturn.class)
            .setParameter("id", id).getResultList();
        return new LifecycleView(
            sale.id, sale.status, sale.organizationName, sale.unitName, sale.buyerName,
            sale.processNumber, sale.legalBasis, sale.documentReference,
            sale.finalizedAt == null ? null : sale.finalizedAt.toString(),
            sale.withdrawalState, sale.withdrawnAt == null ? null : sale.withdrawnAt.toString(), sale.withdrawnByLogin,
            sale.titleTransferState, sale.titleTransferredAt == null ? null : sale.titleTransferredAt.toString(),
            sale.total.toPlainString(),
            items.stream().map(i -> new ItemView(
                i.id, i.stockCode, i.modelName, i.asset == null ? "LOT" : "ASSET",
                i.quantity.toPlainString(), i.unitPrice.toPlainString(), i.subtotal.toPlainString(),
                i.previousOwnerType, i.previousOwnerName, i.newOwnerType, i.newOwnerName,
                i.movement.id, i.movement.nature,
                i.asset == null ? null : i.asset.status,
                returnedQuantity(i.id).toPlainString()
            )).toList(),
            returns.stream().map(r -> new ReturnView(
                r.id, r.cancellation, r.reason.code, r.reason.name, r.returnedAt.toString(),
                r.refundAmount.toPlainString(), r.refundReference, r.operatorLogin,
                em.createQuery("select count(i) from SaleReturnItem i where i.saleReturn.id=:id", Long.class)
                    .setParameter("id", r.id).getSingleResult()
            )).toList()
        );
    }

    private BigDecimal returnedQuantity(long itemId) {
        BigDecimal value = em.createQuery(
            "select sum(i.quantity) from SaleReturnItem i where i.saleItem.id=:id", BigDecimal.class)
            .setParameter("id", itemId).getSingleResult();
        return value == null ? BigDecimal.ZERO : value;
    }

    public record LifecycleView(
        long id, String status, String organizationName, String unitName, String buyerName,
        String processNumber, String legalBasis, String documentReference,
        String finalizedAt, String withdrawalState, String withdrawnAt, String withdrawnByLogin,
        String titleTransferState, String titleTransferredAt, String total,
        List<ItemView> items, List<ReturnView> returns
    ) {}

    public record ItemView(
        long id, String stockCode, String modelName, String kind,
        String quantity, String unitPrice, String subtotal,
        String previousOwnerType, String previousOwnerName, String newOwnerType, String newOwnerName,
        long movementId, String movementNature, String currentAssetStatus, String returnedQuantity
    ) {}

    public record ReturnView(
        long id, boolean cancellation, String reasonCode, String reasonName, String returnedAt,
        String refundAmount, String refundReference, String operatorLogin, long itemCount
    ) {}
}
