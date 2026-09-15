package com.weaponsregistration.purchase.controller;

import com.weaponsregistration.purchase.dto.EquipmentReceivingContract;
import com.weaponsregistration.purchase.model.EquipmentReceiving;
import com.weaponsregistration.purchase.service.EquipmentReceivingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/erp/receivings")
public class EquipmentReceivingController {
    private final EquipmentReceivingService service;
    public EquipmentReceivingController(EquipmentReceivingService service) { this.service = service; }

    @GetMapping
    public List<EquipmentReceiving> list(@RequestParam(required = false) Long acquisitionId) {
        return service.list(acquisitionId);
    }

    @GetMapping("/{id}")
    public EquipmentReceiving get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentReceiving create(@RequestBody EquipmentReceivingContract.CreateRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    public EquipmentReceiving changeStatus(
        @PathVariable Long id,
        @RequestBody EquipmentReceivingContract.StatusRequest request
    ) { return service.changeStatus(id, request); }
}