package com.comandos.demo;

import com.comandos.core.model.Organization;
import com.comandos.core.model.Person;
import com.comandos.inventory.model.ItemModel;
import com.comandos.inventory.model.StockLocation;
import com.comandos.inventory.model.StockLot;
import com.comandos.purchase.model.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(45)
public class PurchaseDemoSeeder implements ApplicationRunner {

    private final EntityManager em;

    public PurchaseDemoSeeder(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Organization buyer = one(Organization.class, "acronym", "SSP-DEMO");
        Organization supplier = one(Organization.class, "acronym", "FBD");
        Person origin = one(Person.class, "taxId", "22222222222");
        ItemModel model = one(ItemModel.class, "sku", "CBC-9-LUGER-FMJ");
        StockLocation location = one(StockLocation.class, "code", "ARM-COFRE-01");
        StockLot lot = one(StockLot.class, "lotNumber", "CBC-DEMO-2026-001");
        if (buyer == null || supplier == null || origin == null || model == null || location == null || lot == null) return;

        PurchasePlanning planning = seedPlanning(buyer);
        ProcurementProcess process = seedProcurement(buyer);
        Purchase purchase = seedPurchase(buyer, supplier, origin, process, model);
        PurchaseItem item = first(PurchaseItem.class);
        seedAcquisitionDocument(purchase);
        seedReceipt(purchase, item);
        seedEquipmentReceiving(buyer, purchase, item, model, location, lot);
        em.flush();
    }

    private PurchasePlanning seedPlanning(Organization organization) {
        PurchasePlanning existing = first(PurchasePlanning.class);
        if (existing != null) return existing;

        PurchasePlanning value = new PurchasePlanning();
        value.organization = organization;
        value.processNumber = "PLANEJ-DEMO-2026-001";
        value.requestingUnit = "Gerência de Armamento";
        value.objectDescription = "Aquisição fictícia de munição 9 mm para treinamento institucional.";
        value.needJustification = "Reposição didática do estoque para demonstrar o ciclo completo de compras.";
        value.estimatedValue = new BigDecimal("45000.00");
        value.expectedContractDate = LocalDate.now().plusMonths(2);
        value.budgetSource = "Dotação orçamentária fictícia - treinamento";
        value.budgetAvailable = true;
        value.technicalPreliminaryStudyRequired = true;
        value.termsOfReferenceRequired = true;
        value.riskAnalysisRequired = true;
        value.priceResearchRequired = true;
        value.status = PurchasePlanningStatus.APPROVED;
        value.notes = "Exemplo de planejamento aprovado para treinamento do COMANDOS.";
        em.persist(value);

        PurchasePlanningDocument document = new PurchasePlanningDocument();
        document.planning = value;
        document.documentType = PurchasePlanningDocumentType.TECHNICAL_PRELIMINARY_STUDY;
        document.title = "Estudo Técnico Preliminar - aquisição de munição 9 mm";
        document.documentNumber = "ETP-DEMO-001/2026";
        document.documentUrl = "https://example.local/comandos/documentos/etp-demo-001";
        document.legalBasis = "Referência normativa fictícia para fins didáticos.";
        document.issuedAt = LocalDateTime.now().minusDays(45);
        document.approved = true;
        document.notes = "Documento fictício.";
        em.persist(document);

        PurchasePlanningApproval approval = new PurchasePlanningApproval();
        approval.planning = value;
        approval.stepName = "Aprovação da necessidade";
        approval.responsibleLogin = "autoridade.demo";
        approval.decision = "APPROVED";
        approval.decidedAt = LocalDateTime.now().minusDays(40);
        approval.justification = "Necessidade aprovada para cenário de treinamento.";
        em.persist(approval);
        return value;
    }

    private ProcurementProcess seedProcurement(Organization organization) {
        ProcurementProcess existing = first(ProcurementProcess.class);
        if (existing != null) return existing;

        ProcurementProcess value = new ProcurementProcess();
        value.organization = organization;
        value.processNumber = "PROC-DEMO-2026-0001";
        value.objectDescription = "Aquisição de 10.000 cartuchos CBC 9 mm Luger FMJ - cenário de demonstração.";
        value.justification = "Reposição fictícia de estoque para treinamento do fluxo de contratação.";
        value.procurementMethod = ProcurementMethod.BIDDING;
        value.biddingModality = BiddingModality.PREGAO;
        value.status = ProcurementStatus.HOMOLOGATED;
        value.estimatedValue = new BigDecimal("45000.00");
        value.legalBasis = "Referência legal fictícia para treinamento.";
        value.openingDate = LocalDate.now().minusDays(35);
        value.publicationDate = LocalDate.now().minusDays(30);
        value.awardDate = LocalDate.now().minusDays(15);
        value.homologationDate = LocalDate.now().minusDays(10);
        em.persist(value);

        ProcurementDocument document = new ProcurementDocument();
        document.procurementProcess = value;
        document.documentType = "EDITAL";
        document.documentNumber = "EDITAL-DEMO-001/2026";
        document.title = "Pregão fictício - munição 9 mm";
        document.storageReference = "SEI-DEMO-COMPRAS-001";
        document.legalRequirement = true;
        document.active = true;
        em.persist(document);
        return value;
    }

    private Purchase seedPurchase(Organization buyer, Organization supplier, Person origin,
                                  ProcurementProcess process, ItemModel model) {
        Purchase existing = first(Purchase.class);
        if (existing != null) return existing;

        Purchase value = new Purchase();
        value.buyerOrganization = buyer;
        value.supplierOrganization = supplier;
        value.originPerson = origin;
        value.procurementProcess = process;
        value.acquisitionType = AcquisitionType.ONEROUS;
        value.originDescription = "Compra decorrente do processo demonstrativo PROC-DEMO-2026-0001.";
        value.purchaseNumber = "COMPRA-DEMO-2026-001";
        value.purchaseDate = LocalDate.now().minusDays(8);
        value.status = PurchaseStatus.RECEIVED;
        value.discount = BigDecimal.ZERO;
        value.freight = BigDecimal.ZERO;
        value.taxes = BigDecimal.ZERO;
        value.otherCosts = BigDecimal.ZERO;
        value.paymentConditions = "Pagamento fictício em 30 dias após o recebimento definitivo.";
        value.deliveryConditions = "Entrega no Cofre Central - ambiente de demonstração.";
        value.warrantyConditions = "Condições meramente ilustrativas.";
        value.notes = "Compra fictícia para treinamento.";
        em.persist(value);

        PurchaseItem item = new PurchaseItem();
        item.purchase = value;
        item.itemModel = model;
        item.quantity = new BigDecimal("10000");
        item.receivedQuantity = new BigDecimal("10000");
        item.unitPrice = new BigDecimal("4.50");
        item.discount = BigDecimal.ZERO;
        item.conditionDescription = "Produto novo, embalagem original.";
        item.notes = "Item didático vinculado ao modelo real CBC 9 mm Luger FMJ.";
        em.persist(item);
        value.items.add(item);
        value.recalculateTotals();
        return value;
    }

    private void seedAcquisitionDocument(Purchase purchase) {
        if (count(AcquisitionDocument.class) > 0) return;
        AcquisitionDocument value = new AcquisitionDocument();
        value.purchase = purchase;
        value.documentType = AcquisitionDocumentType.INVOICE;
        value.documentNumber = "NF-DEMO-000123";
        value.issueDate = LocalDate.now().minusDays(6);
        value.issuer = "Fornecedor Demonstrativo Ltda.";
        value.amount = purchase.total;
        value.storageReference = "SEI-DEMO-NF-000123";
        value.notes = "Nota fiscal fictícia para demonstração.";
        em.persist(value);
    }

    private void seedReceipt(Purchase purchase, PurchaseItem purchaseItem) {
        if (purchaseItem == null || count(PurchaseReceipt.class) > 0) return;
        PurchaseReceipt receipt = new PurchaseReceipt();
        receipt.purchase = purchase;
        receipt.receiptNumber = "REC-DEMO-001";
        receipt.receiptDate = LocalDate.now().minusDays(5);
        receipt.invoiceNumber = "NF-DEMO-000123";
        receipt.invoiceKey = "CHAVE-FICTICIA-NAO-FISCAL-000123";
        receipt.status = ReceiptStatus.DEFINITIVELY_RECEIVED;
        receipt.inspectionNotes = "Quantidade, lote e integridade conferidos no cenário didático.";
        em.persist(receipt);

        PurchaseReceiptItem item = new PurchaseReceiptItem();
        item.receipt = receipt;
        item.purchaseItem = purchaseItem;
        item.receivedQuantity = purchaseItem.quantity;
        item.acceptedQuantity = purchaseItem.quantity;
        item.rejectedQuantity = BigDecimal.ZERO;
        em.persist(item);
    }

    private void seedEquipmentReceiving(Organization organization, Purchase purchase, PurchaseItem purchaseItem,
                                        ItemModel model, StockLocation location, StockLot lot) {
        if (purchaseItem == null || count(EquipmentReceiving.class) > 0) return;

        EquipmentReceiving receiving = new EquipmentReceiving();
        receiving.sourceType = ReceivingSourceType.ACQUISITION;
        receiving.acquisition = purchase;
        receiving.receivingOrganization = organization;
        receiving.receivingUnit = "Gerência de Armamento";
        receiving.receivingLocation = location.name;
        receiving.deliveryDocumentNumber = "ROMANEIO-DEMO-001";
        receiving.invoiceNumber = "NF-DEMO-000123";
        receiving.receivedAt = LocalDateTime.now().minusDays(5);
        receiving.receivedBy = "maria.armeira.demo";
        receiving.physicalChecked = true;
        receiving.documentsChecked = true;
        receiving.status = ReceivingStatus.DEFINITIVELY_ACCEPTED;
        receiving.notes = "Recebimento fictício completo para entendimento do fluxo.";
        em.persist(receiving);

        EquipmentReceivingItem item = new EquipmentReceivingItem();
        item.receiving = receiving;
        item.acquisitionItem = purchaseItem;
        item.itemModelId = model.id;
        item.expectedQuantity = new BigDecimal("10000");
        item.receivedQuantity = new BigDecimal("10000");
        item.acceptedQuantity = new BigDecimal("10000");
        item.rejectedQuantity = BigDecimal.ZERO;
        item.lotNumber = "CBC-DEMO-2026-001";
        item.manufactureDate = LocalDate.now().minusMonths(3);
        item.expirationDate = LocalDate.now().plusYears(4);
        item.conditionDescription = "Embalagens íntegras e lote legível.";
        item.notes = "Item de recebimento didático.";
        em.persist(item);

        ReceivingSerial serial = new ReceivingSerial();
        serial.receivingItem = item;
        serial.serialNumber = "SERIE-DEMO-CAIXA-001";
        serial.manufacturerCode = "CBC-DEMO";
        serial.assetCode = "NAO-SERIALIZADO";
        serial.accepted = true;
        em.persist(serial);

        ReceivingInspection inspection = new ReceivingInspection();
        inspection.receiving = receiving;
        inspection.inspectedAt = LocalDateTime.now().minusDays(4);
        inspection.inspector = "maria.armeira.demo";
        inspection.provisionalReceipt = true;
        inspection.definitiveReceipt = true;
        inspection.approved = true;
        inspection.decisionNotes = "Recebimento aprovado no cenário didático.";
        em.persist(inspection);

        ReceivingIncorporation incorporation = new ReceivingIncorporation();
        incorporation.receiving = receiving;
        incorporation.receivingItem = item;
        incorporation.receivingSerial = serial;
        incorporation.stockLocationId = location.id;
        incorporation.lotNumber = lot.lotNumber;
        incorporation.quantity = new BigDecimal("10000");
        incorporation.incorporationValue = new BigDecimal("45000.00");
        incorporation.initialCondition = "NEW";
        incorporation.inventoryResource = "lots";
        incorporation.inventoryRecordId = lot.id;
        incorporation.incorporatedBy = "maria.armeira.demo";
        incorporation.incorporatedAt = LocalDateTime.now().minusDays(4);
        incorporation.notes = "Incorporação fictícia associada ao lote já presente na base demo.";
        em.persist(incorporation);
    }

    private <T> T one(Class<T> type, String field, Object value) {
        return em.createQuery("select e from " + type.getSimpleName() + " e where e." + field + " = :value", type)
            .setParameter("value", value)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private <T> T first(Class<T> type) {
        return em.createQuery("select e from " + type.getSimpleName() + " e order by e.id", type)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private long count(Class<?> type) {
        return em.createQuery("select count(e) from " + type.getSimpleName() + " e", Long.class)
            .getSingleResult();
    }
}
