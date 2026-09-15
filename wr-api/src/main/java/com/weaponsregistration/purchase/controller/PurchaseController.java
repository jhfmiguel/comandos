package com.weaponsregistration.purchase.controller;

import com.weaponsregistration.purchase.dto.PurchaseContract.*;
import com.weaponsregistration.purchase.service.PurchaseService;
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

    @PostMapping("/{id}/cancel")
    public PurchaseView cancel(@PathVariable Long id) { return service.cancel(id); }
}