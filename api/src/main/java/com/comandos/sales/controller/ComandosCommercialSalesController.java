package com.comandos.sales.controller;

import com.comandos.security.service.ComandosAccessPolicy;
import com.fariamiguel.app.audit.service.AuditService;
import com.fariamiguel.enterprise.sales.SalesOperationsDirectory;
import com.fariamiguel.enterprise.sales.SalesOperationsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Product adapter for COMANDOS commercial sales, retaining its organization/unit authorization. */
@RestController
@RequestMapping("/api/erp/commercial-sales/orders")
public class ComandosCommercialSalesController {
    private static final String TENANT = "comandos";
    private final SalesOperationsService sales;
    private final ComandosAccessPolicy access;
    private final AuditService audit;

    public ComandosCommercialSalesController(SalesOperationsService sales, ComandosAccessPolicy access,
            AuditService audit) {
        this.sales = Objects.requireNonNull(sales);
        this.access = Objects.requireNonNull(access);
        this.audit = Objects.requireNonNull(audit);
    }

    @PostMapping
    @Transactional
    public SalesOperationsDirectory.SalesDocument create(@RequestBody CreateOrder request) {
        validate(request);
        access.requireScope("sales", "CREATE", request.organizationId(), request.unitId());
        Map<String, String> metadata = new HashMap<>();
        metadata.put("comandos.organizationId", Long.toString(request.organizationId()));
        if (request.unitId() != null) metadata.put("comandos.unitId", request.unitId().toString());
        metadata.put("comandos.buyerId", Long.toString(request.buyerId()));
        metadata.put("comandos.paymentMethod", request.paymentMethod());
        metadata.put("comandos.processNumber", request.processNumber());
        metadata.put("comandos.legalBasis", request.legalBasis());
        metadata.put("comandos.documentReference", request.documentReference());
        List<SalesOperationsDirectory.Line> lines = request.lines().stream()
                .map(line -> new SalesOperationsDirectory.Line(line.itemId(), line.description(), line.quantity(),
                        line.unitPrice(), BigDecimal.ZERO)).toList();
        return sales.createDocument(TENANT, request.reference(), request.customerId(),
                SalesOperationsDirectory.DocumentType.ORDER, lines, "BRL", actor(), metadata);
    }

    @GetMapping
    public List<SalesOperationsDirectory.SalesDocument> list(
            @RequestParam(required = false) SalesOperationsDirectory.DocumentStatus status,
            @RequestParam Long organizationId, @RequestParam(required = false) Long unitId) {
        access.requireScope("sales", "READ", organizationId, unitId);
        return sales.listDocuments(TENANT, status).stream()
                .filter(document -> scopeMatches(document, organizationId, unitId)).toList();
    }

    @GetMapping("/{id}")
    public SalesOperationsDirectory.SalesDocument get(@PathVariable String id) {
        var document = document(id);
        requireScope(document, "READ");
        return document;
    }

    @PostMapping("/{id}/submit")
    @Transactional
    public SalesOperationsDirectory.SalesDocument submit(@PathVariable String id) {
        var document = document(id);
        requireScope(document, "UPDATE");
        return sales.submitDocument(TENANT, id, actor());
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public SalesOperationsDirectory.SalesDocument approve(@PathVariable String id) {
        var document = document(id);
        requireScope(document, "APPROVE");
        return sales.approveDocument(TENANT, id, actor());
    }

    @PostMapping("/{id}/confirm")
    @Transactional
    public SalesOperationsDirectory.SalesDocument confirm(@PathVariable String id) {
        var document = document(id);
        requireScope(document, "CONFIRM");
        return sales.confirmDocument(TENANT, id, actor());
    }

    @PostMapping("/{id}/invoice")
    @Transactional
    public SalesOperationsDirectory.SalesInvoice invoice(@PathVariable String id, @RequestBody IssueInvoice body) {
        var document = document(id);
        requireScope(document, "INVOICE");
        return sales.invoiceDocument(TENANT, id, body.issuedOn(), body.dueOn(), actor());
    }

    @PostMapping("/{id}/deliveries")
    @Transactional
    public SalesOperationsDirectory.SalesDelivery deliver(@PathVariable String id, @RequestBody Delivery body) {
        var document = document(id);
        requireScope(document, "UPDATE");
        return sales.recordDelivery(TENANT, id, body.reference(), actor(), body.lines(), body.metadata());
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public SalesOperationsDirectory.SalesDocument cancel(@PathVariable String id, @RequestBody Cancel body) {
        var document = document(id);
        requireScope(document, "UPDATE");
        return sales.cancelDocument(TENANT, id, actor(), body.reason());
    }

    private SalesOperationsDirectory.SalesDocument document(String id) {
        return sales.findDocument(TENANT, id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Sales order not found"));
    }

    private void requireScope(SalesOperationsDirectory.SalesDocument document, String action) {
        Scope scope = scope(document);
        access.requireScope("sales", action, scope.organizationId(), scope.unitId());
    }

    private static boolean scopeMatches(SalesOperationsDirectory.SalesDocument document, long organizationId,
            Long unitId) {
        Scope stored = scope(document);
        return stored.organizationId() == organizationId && Objects.equals(stored.unitId(), unitId);
    }

    private static Scope scope(SalesOperationsDirectory.SalesDocument document) {
        try {
            long organization = Long.parseLong(document.metadata().get("comandos.organizationId"));
            String rawUnit = document.metadata().get("comandos.unitId");
            Long unit = rawUnit == null ? null : Long.parseLong(rawUnit);
            if (organization <= 0 || unit != null && unit <= 0) throw new NumberFormatException();
            return new Scope(organization, unit);
        } catch (RuntimeException malformed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Sales document has no valid COMANDOS organization/unit scope");
        }
    }

    private String actor() {
        var current = audit.actor();
        if (current.id() == null || current.login() == null)
            throw new org.springframework.security.access.AccessDeniedException("Authenticated COMANDOS actor is required");
        return current.id().toString();
    }

    private static void validate(CreateOrder request) {
        if (request == null || request.organizationId() == null || request.organizationId() <= 0
                || request.buyerId() == null || request.buyerId() <= 0 || blank(request.reference())
                || blank(request.customerId()) || blank(request.paymentMethod()) || blank(request.processNumber())
                || blank(request.legalBasis()) || blank(request.documentReference())
                || request.lines() == null || request.lines().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Required sales order fields are missing");
        if (request.unitId() != null && request.unitId() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unitId must be positive");
        try {
            com.fariamiguel.app.model.PaymentMethod.valueOf(request.paymentMethod());
        } catch (RuntimeException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment method");
        }
        try {
            if (Long.parseLong(request.customerId()) != request.buyerId())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "customerId and buyerId must resolve to the same COMANDOS person");
        } catch (NumberFormatException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "customerId must be the COMANDOS person identifier used by buyerId");
        }
        if (request.lines().stream().anyMatch(line -> line == null || blank(line.itemId())
                || line.quantity() == null || line.quantity().signum() <= 0
                || line.unitPrice() == null || line.unitPrice().signum() < 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sales order lines are invalid");
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.web.server.ResponseStatusException invalidOrder(IllegalArgumentException failure) {
        return new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, failure.getMessage(), failure);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public org.springframework.web.server.ResponseStatusException invalidTransition(IllegalStateException failure) {
        return new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, failure.getMessage(), failure);
    }

    public record CreateOrder(String reference, String customerId, Long organizationId, Long unitId,
            Long buyerId, String paymentMethod, String processNumber, String legalBasis,
            String documentReference, List<OrderLine> lines) { }
    public record OrderLine(String itemId, String description, BigDecimal quantity, BigDecimal unitPrice) { }
    public record IssueInvoice(LocalDate issuedOn, LocalDate dueOn) { }
    public record Delivery(String reference, List<SalesOperationsDirectory.FulfilledLine> lines,
            Map<String, String> metadata) { }
    public record Cancel(String reason) { }
    private record Scope(long organizationId, Long unitId) { }
}
