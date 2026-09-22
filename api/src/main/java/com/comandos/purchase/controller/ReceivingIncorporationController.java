package com.comandos.purchase.controller;

import com.comandos.purchase.dto.ReceivingIncorporationContract;
import com.comandos.purchase.service.ReceivingIncorporationService;
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
    public List<ReceivingIncorporationContract.View> list(
        @RequestParam(required = false) Long receivingId
    ) {
        return service.list(receivingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReceivingIncorporationContract.View create(
        @RequestBody ReceivingIncorporationContract.CreateRequest request
    ) {
        return service.create(request);
    }
}
