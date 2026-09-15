package com.weaponsregistration.purchase.service;

import com.weaponsregistration.core.model.Organization;
import com.weaponsregistration.inventory.model.ItemModel;
import com.weaponsregistration.purchase.dto.PurchaseContract.*;
import com.weaponsregistration.purchase.model.*;
import com.weaponsregistration.purchase.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PurchaseService {
    private final PurchaseRepository purchaseRepository;
    private final ProcurementProcessRepository procurementRepository;
    private final EntityManager entityManager;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           ProcurementProcessRepository procurementRepository,
                           EntityManager entityManager) {
        this.purchaseRepository = purchaseRepository;
        this.procurementRepository = procurementRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public PurchaseView create(CreatePurchaseRequest request) {
        if (request == null || request.buyerOrganizationId() == null)
            throw new IllegalArgumentException("Buyer organization is required.");
        if (request.purchaseNumber() == null || request.purchaseNumber().isBlank())
            throw new IllegalArgumentException("Purchase number is required.");
        if (request.items() == null || request.items().isEmpty())
            throw new IllegalArgumentException("Purchase requires at least one item.");

        Organization buyer = findOrganization(request.buyerOrganizationId());
        Organization supplier = request.supplierOrganizationId() == null
            ? null : findOrganization(request.supplierOrganizationId());

        Purchase purchase = new Purchase();
        purchase.buyerOrganization = buyer;
        purchase.supplierOrganization = supplier;
        purchase.purchaseNumber = request.purchaseNumber().trim();
        purchase.purchaseDate = request.purchaseDate() == null ? LocalDate.now() : request.purchaseDate();
        purchase.notes = request.notes();

        for (CreatePurchaseItemRequest r : request.items()) {
            if (r.itemModelId() == null || r.quantity() == null ||
                r.quantity().compareTo(BigDecimal.ZERO) <= 0)
                throw new IllegalArgumentException("Valid item and quantity are required.");
            if (r.unitPrice() == null || r.unitPrice().compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("Unit price cannot be negative.");

            ItemModel model = entityManager.find(ItemModel.class, r.itemModelId());
            if (model == null) throw new EntityNotFoundException("ItemModel not found: " + r.itemModelId());

            PurchaseItem item = new PurchaseItem();
            item.purchase = purchase;
            item.itemModel = model;
            item.quantity = r.quantity();
            item.unitPrice = r.unitPrice();
            item.discount = r.discount() == null ? BigDecimal.ZERO : r.discount();
            item.notes = r.notes();
            purchase.items.add(item);
        }

        purchase.recalculateTotals();
        return toView(purchaseRepository.save(purchase));
    }

    @Transactional
    public PurchaseView configureProcurement(Long purchaseId, CreateProcurementRequest request) {
        Purchase purchase = findPurchase(purchaseId);
        if (request == null || request.procurementMethod() == null)
            throw new IllegalArgumentException("Procurement method is required.");

        boolean isPublic = Boolean.TRUE.equals(purchase.buyerOrganization.publicOrganization);
        if (isPublic && request.procurementMethod() == ProcurementMethod.NOT_REQUIRED)
            throw new IllegalArgumentException("Public organization requires procurement.");
        if (!isPublic && request.procurementMethod() != ProcurementMethod.NOT_REQUIRED)
            throw new IllegalArgumentException("Private organization does not require public procurement.");
        if (request.procurementMethod() == ProcurementMethod.BIDDING && request.biddingModality() == null)
            throw new IllegalArgumentException("Bidding modality is required.");
        if (request.procurementMethod() == ProcurementMethod.DIRECT_CONTRACTING &&
            request.directContractingType() == null)
            throw new IllegalArgumentException("Direct contracting type is required.");
        if (request.procurementMethod() == ProcurementMethod.DIRECT_CONTRACTING &&
            (request.legalBasis() == null || request.legalBasis().isBlank()))
            throw new IllegalArgumentException("Legal basis is required.");

        ProcurementProcess process = purchase.procurementProcess == null
            ? new ProcurementProcess() : purchase.procurementProcess;
        process.organization = purchase.buyerOrganization;
        process.processNumber = request.processNumber();
        process.objectDescription = request.objectDescription();
        process.justification = request.justification();
        process.procurementMethod = request.procurementMethod();
        process.biddingModality = request.biddingModality();
        process.directContractingType = request.directContractingType();
        process.estimatedValue = request.estimatedValue() == null ? purchase.total : request.estimatedValue();
        process.legalBasis = request.legalBasis();
        process.supplierChoiceReason = request.supplierChoiceReason();
        process.priceJustification = request.priceJustification();

        purchase.procurementProcess = procurementRepository.save(process);
        purchase.status = request.procurementMethod() == ProcurementMethod.NOT_REQUIRED
            ? PurchaseStatus.AUTHORIZED : PurchaseStatus.PROCUREMENT_IN_PROGRESS;
        return toView(purchaseRepository.save(purchase));
    }

    @Transactional(readOnly = true)
    public PurchaseView get(Long id) { return toView(findPurchase(id)); }

    @Transactional(readOnly = true)
    public List<PurchaseView> list(Long organizationId) {
        return purchaseRepository.findByBuyerOrganizationIdOrderByCreatedAtDesc(organizationId)
            .stream().map(this::toView).toList();
    }

    @Transactional
    public PurchaseView cancel(Long id) {
        Purchase purchase = findPurchase(id);
        if (purchase.status == PurchaseStatus.RECEIVED)
            throw new IllegalStateException("Received purchase cannot be cancelled.");
        purchase.status = PurchaseStatus.CANCELLED;
        return toView(purchaseRepository.save(purchase));
    }

    private Organization findOrganization(Long id) {
        Organization value = entityManager.find(Organization.class, id);
        if (value == null) throw new EntityNotFoundException("Organization not found: " + id);
        return value;
    }

    private Purchase findPurchase(Long id) {
        return purchaseRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found: " + id));
    }

    private PurchaseView toView(Purchase p) {
        ProcurementView procurement = null;
        if (p.procurementProcess != null) {
            ProcurementProcess x = p.procurementProcess;
            procurement = new ProcurementView(x.id, x.processNumber, x.procurementMethod,
                x.biddingModality, x.directContractingType, x.status, x.estimatedValue, x.legalBasis);
        }
        List<PurchaseItemView> items = p.items.stream()
            .map(i -> new PurchaseItemView(i.id, i.itemModel.id, i.itemModel.name,
                i.quantity, i.receivedQuantity, i.unitPrice, i.discount, i.calculateTotal()))
            .toList();
        return new PurchaseView(p.id, p.buyerOrganization.id, p.buyerOrganization.name,
            p.supplierOrganization == null ? null : p.supplierOrganization.id,
            p.supplierOrganization == null ? null : p.supplierOrganization.name,
            p.purchaseNumber, p.purchaseDate, p.status, p.subtotal, p.discount,
            p.freight, p.taxes, p.total, procurement, items);
    }
}