package com.weaponsregistration.purchase.controller;

import com.weaponsregistration.purchase.dto.ReceivingIncorporationContract;
import com.weaponsregistration.purchase.model.ReceivingIncorporation;
import com.weaponsregistration.purchase.service.ReceivingIncorporationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/erp/receiving-incorporations")
public class ReceivingIncorporationController {
    private final ReceivingIncorporationService service;

    public ReceivingIncorporationController(ReceivingIncorporationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReceivingIncorporation> list(@RequestParam(required = false) Long receivingId) {
        return service.list(receivingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceivingIncorporation create(
        @RequestBody ReceivingIncorporationContract.CreateRequest request
    ) {
        return service.create(request);
    }
}