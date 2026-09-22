package com.comandos.purchase.controller;

import com.comandos.purchase.dto.PurchaseContract.*;
import com.comandos.purchase.service.PurchaseService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/erp/purchases")
public class PurchaseController {
    private final PurchaseService service;
    public PurchaseController(PurchaseService service) { this.service = service; }

    @PostMapping
    public PurchaseView create(@RequestBody CreatePurchaseRequest request) { return service.create(request); }

    @GetMapping("/{id}")
    public PurchaseView get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    public List<PurchaseView> list(@RequestParam Long organizationId) { return service.list(organizationId); }

    @PutMapping("/{id}/procurement")
    public PurchaseView configureProcurement(@PathVariable Long id,
        @RequestBody CreateProcurementRequest request) {
        return service.configureProcurement(id, request);
    }

    @PutMapping("/{id}/procurement/status")
    public PurchaseView updateProcurementStatus(@PathVariable Long id,
        @RequestBody ProcurementStatusRequest request) {
        return service.updateProcurementStatus(id, request);
    }

    @PostMapping("/{id}/authorize")
    public PurchaseView authorize(@PathVariable Long id) { return service.authorize(id); }

    @PostMapping("/{id}/order")
    public PurchaseView order(@PathVariable Long id) { return service.order(id); }

    @PostMapping("/{id}/cancel")
    public PurchaseView cancel(@PathVariable Long id) { return service.cancel(id); }
}