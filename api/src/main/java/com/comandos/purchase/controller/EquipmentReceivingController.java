package com.comandos.purchase.controller;

import com.comandos.purchase.dto.EquipmentReceivingContract;
import com.comandos.purchase.service.EquipmentReceivingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/erp/receivings")
public class EquipmentReceivingController {
    private final EquipmentReceivingService service;

    public EquipmentReceivingController(EquipmentReceivingService service) {
        this.service = service;
    }

    @GetMapping
    public List<EquipmentReceivingContract.View> list(
        @RequestParam(required = false) Long acquisitionId
    ) {
        return service.list(acquisitionId);
    }

    @GetMapping("/{id}")
    public EquipmentReceivingContract.View get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentReceivingContract.View create(
        @RequestBody EquipmentReceivingContract.CreateRequest request
    ) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    public EquipmentReceivingContract.View changeStatus(
        @PathVariable Long id,
        @RequestBody EquipmentReceivingContract.StatusRequest request
    ) {
        return service.changeStatus(id, request);
    }
}
