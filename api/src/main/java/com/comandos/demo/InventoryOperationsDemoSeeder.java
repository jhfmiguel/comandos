package com.comandos.demo;

import com.comandos.core.model.*;
import com.comandos.inventory.model.*;
import com.comandos.reconciliation.model.*;
import com.comandos.reservation.model.*;
import com.comandos.transfer.model.*;
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
@Order(50)
public class InventoryOperationsDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public InventoryOperationsDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization org = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit central = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");
        OrganizationalUnit operational = one(OrganizationalUnit.class, "code", "UOP-01");
        StockLocation centralVault = one(StockLocation.class, "code", "ARM-COFRE-01");
        StockLocation operationalVault = one(StockLocation.class, "code", "UOP-COFRE-01");
        AssetItem asset = one(AssetItem.class, "assetCode", "PAT-DEMO-0001");
        StockLot lot = one(StockLot.class, "lotNumber", "CBC-DEMO-2026-001");
        StockBalance balance = lot == null || centralVault == null ? null : balance(lot, centralVault);

        if (org == null || central == null || operational == null || centralVault == null
                || operationalVault == null || asset == null || lot == null || balance == null) return;

        seedReservation(org, operational, lot, balance, centralVault);
        seedTransfer(org, central, operational, asset, centralVault, operationalVault);
        seedInventoryCount(org, central, centralVault, lot, balance);
        em.flush();
    }

    private void seedReservation(Organization org, OrganizationalUnit unit, StockLot lot,
                                 StockBalance balance, StockLocation location) {
        if (count(InventoryReservation.class) > 0) return;

        InventoryReservation reservation = new InventoryReservation();
        reservation.organization = org;
        reservation.unit = unit;
        reservation.status = first(ReservationStatusType.class);
        reservation.organizationName = org.name;
        reservation.unitName = unit.name;
        reservation.purpose = "Reserva de 100 munições para treinamento anual - cenário didático";
        reservation.startsAt = LocalDateTime.now().plusDays(7);
        reservation.endsAt = LocalDateTime.now().plusDays(8);
        reservation.createdOn = LocalDateTime.now().minusDays(1);
        reservation.operatorLogin = "maria.armeira.demo";
        reservation.requestId = id("reservation");
        reservation.requestFingerprint = fingerprint("reservation");
        em.persist(reservation);

        ReservationItem item = new ReservationItem();
        item.reservation = reservation;
        item.model = lot.model;
        item.lot = lot;
        item.balance = balance;
        item.location = location;
        item.modelName = lot.model.name;
        item.sku = lot.model.sku;
        item.stockCode = lot.lotNumber;
        item.locationName = location.name;
        item.unitOfMeasure = lot.model.unitOfMeasure;
        item.quantity = new BigDecimal("100");
        em.persist(item);
    }

    private void seedTransfer(Organization org, OrganizationalUnit source, OrganizationalUnit destination,
                              AssetItem asset, StockLocation sourceLocation, StockLocation destinationLocation) {
        if (count(InventoryTransfer.class) > 0) return;

        InventoryTransfer transfer = new InventoryTransfer();
        transfer.organization = org;
        transfer.sourceUnit = source;
        transfer.destinationUnit = destination;
        transfer.destinationLocation = destinationLocation;
        transfer.organizationName = org.name;
        transfer.sourceUnitName = source.name;
        transfer.destinationUnitName = destination.name;
        transfer.destinationLocationName = destinationLocation.name;
        transfer.purpose = "Redistribuição interna de equipamento - cenário didático";
        transfer.transferType = "INTERNAL";
        transfer.legalInstrument = "Ordem de movimentação DEMO-001";
        transfer.documentReference = "SEI-DEMO-TRANSFER-001";
        transfer.approvedByLogin = "autoridade.demo";
        transfer.approvedAt = LocalDateTime.now().minusDays(12);
        transfer.status = "FINALIZED";
        transfer.sentAt = LocalDateTime.now().minusDays(11);
        transfer.finalizedByLogin = "maria.armeira.demo";
        transfer.requestId = id("transfer");
        transfer.requestFingerprint = fingerprint("transfer");
        em.persist(transfer);

        StockMovement out = movement(asset, null, sourceLocation, "TRANSFER_OUT", BigDecimal.ONE);
        StockMovement in = movement(asset, null, destinationLocation, "TRANSFER_IN", BigDecimal.ONE);

        InventoryTransferItem item = new InventoryTransferItem();
        item.transfer = transfer;
        item.model = asset.model;
        item.asset = asset;
        item.sourceLocation = sourceLocation;
        item.destinationLocation = destinationLocation;
        item.outMovement = out;
        item.inMovement = in;
        item.modelName = asset.model.name;
        item.sku = asset.model.sku;
        item.stockCode = asset.assetCode;
        item.sourceLocationName = sourceLocation.name;
        item.destinationLocationName = destinationLocation.name;
        item.unitOfMeasure = asset.model.unitOfMeasure;
        item.quantity = BigDecimal.ONE;
        em.persist(item);
    }

    private void seedInventoryCount(Organization org, OrganizationalUnit unit, StockLocation location,
                                    StockLot lot, StockBalance balance) {
        if (count(InventoryCount.class) > 0) return;

        InventoryCount inventory = new InventoryCount();
        inventory.organization = org;
        inventory.unit = unit;
        inventory.location = location;
        inventory.status = first(InventoryCountStatusType.class);
        inventory.organizationName = org.name;
        inventory.unitName = unit.name;
        inventory.locationName = location.name;
        inventory.purpose = "Inventário físico mensal - cenário didático";
        inventory.openedAt = LocalDateTime.now().minusDays(3);
        inventory.countedAt = LocalDateTime.now().minusDays(2);
        inventory.approvedAt = LocalDateTime.now().minusDays(1);
        inventory.openedByLogin = "maria.armeira.demo";
        inventory.approvedByLogin = "autoridade.demo";
        inventory.requestId = id("inventory-count");
        inventory.requestFingerprint = fingerprint("inventory-count");
        em.persist(inventory);

        InventoryCountItem item = new InventoryCountItem();
        item.inventoryCount = inventory;
        item.model = lot.model;
        item.lot = lot;
        item.balance = balance;
        item.result = first(InventoryCountResultType.class);
        item.modelName = lot.model.name;
        item.sku = lot.model.sku;
        item.stockCode = lot.lotNumber;
        item.unitOfMeasure = lot.model.unitOfMeasure;
        item.systemQuantity = new BigDecimal("5000");
        item.countedQuantity = new BigDecimal("4998");
        item.differenceQuantity = new BigDecimal("-2");
        item.notes = "Diferença fictícia de 2 unidades para demonstrar conciliação.";
        em.persist(item);
    }

    private StockMovement movement(AssetItem asset, StockLot lot, StockLocation location,
                                   String nature, BigDecimal quantity) {
        StockMovement value = new StockMovement();
        value.asset = asset;
        value.lot = lot;
        value.location = location;
        value.nature = nature;
        value.quantity = quantity;
        value.movedAt = LocalDateTime.now().minusDays(1);
        value.operatorLogin = "maria.armeira.demo";
        value.referenceType = "DEMO";
        value.notes = "Movimentação fictícia para demonstração do fluxo.";
        em.persist(value);
        return value;
    }

    private StockBalance balance(StockLot lot, StockLocation location) {
        return em.createQuery("select b from StockBalance b where b.lot = :lot and b.location = :location", StockBalance.class)
            .setParameter("lot", lot)
            .setParameter("location", location)
            .setMaxResults(1)
            .getResultStream().findFirst().orElse(null);
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value)
            .setMaxResults(1)
            .getResultStream().findFirst().orElse(null);
    }

    private <T> T first(Class<T> type) {
        return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .setMaxResults(1)
            .getResultStream().findFirst().orElse(null);
    }

    private long count(Class<?> type) {
        return em.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class).getSingleResult();
    }

    private String id(String key) {
        return UUID.nameUUIDFromBytes(("comandos-demo-" + key).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String fingerprint(String key) {
        return String.format("%064x", Math.abs(key.hashCode()) + 1000L);
    }
}
