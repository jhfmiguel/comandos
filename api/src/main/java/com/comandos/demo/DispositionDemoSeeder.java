package com.comandos.demo;

import com.comandos.core.model.*;
import com.comandos.disposal.model.*;
import com.comandos.donation.model.*;
import com.comandos.inventory.model.*;
import com.comandos.model.PaymentMethod;
import com.comandos.sales.model.*;
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
@Order(65)
public class DispositionDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public DispositionDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization org = one(Organization.class, "acronym", "SSP-DEMO");
        OrganizationalUnit central = one(OrganizationalUnit.class, "code", "ARM-CENTRAL");
        Person armorer = one(Person.class, "taxId", "22222222222");
        Person recipient = one(Person.class, "taxId", "11111111000191");
        StockLocation centralVault = one(StockLocation.class, "code", "ARM-COFRE-01");
        StockLocation operationalVault = one(StockLocation.class, "code", "UOP-COFRE-01");
        AssetItem donationAsset = one(AssetItem.class, "assetCode", "PAT-DEMO-0003");
        AssetItem disposalAsset = one(AssetItem.class, "assetCode", "PAT-DEMO-0004");
        StockLot lot = one(StockLot.class, "lotNumber", "CBC-DEMO-2026-001");

        if (org == null || central == null || armorer == null || recipient == null
                || centralVault == null || operationalVault == null
                || donationAsset == null || disposalAsset == null || lot == null) return;

        seedDonation(org, central, armorer, recipient, donationAsset, operationalVault);
        seedSale(org, central, recipient, lot, centralVault);
        seedDisposal(org, central, disposalAsset, operationalVault);
        em.flush();
    }

    private void seedDonation(Organization org, OrganizationalUnit unit, Person donor, Person donee,
                              AssetItem asset, StockLocation location) {
        if (count(Donation.class) > 0) return;

        Donation donation = new Donation();
        donation.organization = org;
        donation.unit = unit;
        donation.donor = donor;
        donation.donee = donee;
        donation.organizationName = org.name;
        donation.unitName = unit.name;
        donation.donorName = donor.fullName;
        donation.doneeName = donee.fullName;
        donation.term = "TERMO-DEMO-DOACAO-001";
        donation.direction = "OUTGOING";
        donation.documentReference = "SEI-DEMO-DOACAO-001";
        donation.approvedByLogin = "autoridade.demo";
        donation.approvedAt = LocalDateTime.now().minusDays(40);
        donation.status = "FINALIZED";
        donation.finalizedAt = LocalDateTime.now().minusDays(39);
        donation.finalizedByLogin = "maria.armeira.demo";
        donation.requestId = id("donation");
        donation.requestFingerprint = fingerprint("donation");
        em.persist(donation);

        StockMovement movement = movement(asset, null, location, "DONATION_OUT", BigDecimal.ONE,
            "Saída fictícia por doação para fins de treinamento.");
        DonationItem item = new DonationItem();
        item.donation = donation;
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
    }

    private void seedSale(Organization org, OrganizationalUnit unit, Person buyer,
                          StockLot lot, StockLocation location) {
        if (count(InventorySale.class) > 0) return;

        InventorySale sale = new InventorySale();
        sale.organization = org;
        sale.unit = unit;
        sale.unitName = unit.name;
        sale.buyer = buyer;
        sale.organizationName = org.name;
        sale.buyerName = buyer.fullName;
        sale.paymentMethod = PaymentMethod.BANK_TRANSFER;
        sale.processNumber = "PROC-DEMO-VENDA-001";
        sale.legalBasis = "Processo fictício para demonstração do fluxo de alienação.";
        sale.documentReference = "SEI-DEMO-VENDA-001";
        sale.status = "FINALIZED";
        sale.finalizedAt = LocalDateTime.now().minusDays(60);
        sale.finalizedByLogin = "maria.armeira.demo";
        sale.total = new BigDecimal("225.00");
        sale.requestId = id("sale");
        sale.requestFingerprint = fingerprint("sale");
        em.persist(sale);

        StockMovement movement = movement(null, lot, location, "SALE_OUT", new BigDecimal("50"),
            "Saída fictícia por venda.");
        InventorySaleItem item = new InventorySaleItem();
        item.sale = sale;
        item.model = lot.model;
        item.lot = lot;
        item.location = location;
        item.movement = movement;
        item.modelName = lot.model.name;
        item.sku = lot.model.sku;
        item.stockCode = lot.lotNumber;
        item.locationName = location.name;
        item.unitOfMeasure = lot.model.unitOfMeasure;
        item.quantity = new BigDecimal("50");
        item.unitPrice = new BigDecimal("4.50");
        item.subtotal = new BigDecimal("225.00");
        em.persist(item);

        SaleReturn returned = new SaleReturn();
        returned.sale = sale;
        returned.reason = first(SaleReturnReasonType.class);
        returned.cancellation = false;
        returned.notes = "Devolução parcial fictícia de 10 unidades para demonstrar o fluxo.";
        returned.returnedAt = LocalDateTime.now().minusDays(58);
        returned.refundAmount = new BigDecimal("45.00");
        returned.refundReference = "ESTORNO-DEMO-001";
        returned.operatorLogin = "maria.armeira.demo";
        returned.requestId = id("sale-return");
        returned.requestFingerprint = fingerprint("sale-return");
        em.persist(returned);

        StockMovement returnMovement = movement(null, lot, location, "SALE_RETURN", new BigDecimal("10"),
            "Retorno parcial da venda fictícia.");
        SaleReturnItem returnedItem = new SaleReturnItem();
        returnedItem.saleReturn = returned;
        returnedItem.saleItem = item;
        returnedItem.movement = returnMovement;
        returnedItem.quantity = new BigDecimal("10");
        returnedItem.refundAmount = new BigDecimal("45.00");
        em.persist(returnedItem);
    }

    private void seedDisposal(Organization org, OrganizationalUnit unit, AssetItem asset, StockLocation location) {
        if (count(DisposalProcess.class) > 0) return;

        DisposalProcess process = new DisposalProcess();
        process.organization = org;
        process.unit = unit;
        process.organizationName = org.name;
        process.unitName = unit.name;
        process.processNumber = "PROC-DEMO-DESFAZIMENTO-001";
        process.reason = "Bem fictício considerado antieconômico para demonstrar o fluxo de desfazimento.";
        process.status = "FINALIZED";
        process.finalizedAt = LocalDateTime.now().minusDays(90);
        process.finalizedByLogin = "maria.armeira.demo";
        process.requestId = id("disposal");
        process.requestFingerprint = fingerprint("disposal");
        em.persist(process);

        StockMovement movement = movement(asset, null, location, "DISPOSAL", BigDecimal.ONE,
            "Baixa fictícia para desfazimento.");
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

        Destruction destruction = new Destruction();
        destruction.process = process;
        destruction.method = "Inutilização controlada - exemplo didático";
        destruction.destroyedAt = process.finalizedAt;
        destruction.certificate = "CERT-DEMO-DESTRUICAO-001";
        em.persist(destruction);
    }

    private StockMovement movement(AssetItem asset, StockLot lot, StockLocation location,
                                   String nature, BigDecimal quantity, String notes) {
        StockMovement value = new StockMovement();
        value.asset = asset;
        value.lot = lot;
        value.location = location;
        value.nature = nature;
        value.quantity = quantity;
        value.movedAt = LocalDateTime.now().minusDays(1);
        value.operatorLogin = "maria.armeira.demo";
        value.referenceType = "DEMO";
        value.notes = notes;
        em.persist(value);
        return value;
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
    }

    private <T> T first(Class<T> type) {
        return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .setMaxResults(1).getResultStream().findFirst().orElse(null);
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
