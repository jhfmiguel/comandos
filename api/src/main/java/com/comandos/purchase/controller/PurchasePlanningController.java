package com.comandos.purchase.controller;

import com.comandos.purchase.dto.PurchasePlanningContract.CreateRequest;
import com.comandos.purchase.dto.PurchasePlanningContract.StatusRequest;
import com.comandos.purchase.dto.PurchasePlanningContract.View;
import com.comandos.purchase.service.PurchasePlanningService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/purchase-plannings")
public class PurchasePlanningController {
    private final PurchasePlanningService service;

    public PurchasePlanningController(PurchasePlanningService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public View create(@RequestBody CreateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public View get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<View> list(@RequestParam Long organizationId) {
        return service.list(organizationId);
    }

    @PatchMapping("/{id}/status")
    public View updateStatus(@PathVariable Long id, @RequestBody StatusRequest request) {
        return service.updateStatus(id, request);
    }
}
