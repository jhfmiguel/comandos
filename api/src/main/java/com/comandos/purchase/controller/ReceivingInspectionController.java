package com.comandos.purchase.controller;

import com.comandos.purchase.dto.ReceivingInspectionContract;
import com.comandos.purchase.model.ReceivingInspection;
import com.comandos.purchase.service.ReceivingInspectionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/erp/receiving-inspections")
public class ReceivingInspectionController {
    private final ReceivingInspectionService service;

    public ReceivingInspectionController(ReceivingInspectionService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReceivingInspection> list(@RequestParam(required = false) Long receivingId) {
        return service.list(receivingId);
    }

    @PostMapping("/{receivingId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ReceivingInspection create(
        @PathVariable Long receivingId,
        @RequestBody ReceivingInspectionContract.CreateRequest request
    ) {
        return service.create(receivingId, request);
    }
}