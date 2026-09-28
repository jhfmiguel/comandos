package com.comandos.inventory.controller;

import com.comandos.inventory.dto.EquipmentSetOperationContract.AggregateOperationView;
import com.comandos.inventory.dto.EquipmentSetOperationContract.ConsumableSetRequest;
import com.comandos.inventory.dto.EquipmentSetOperationContract.DisposalSetRequest;
import com.comandos.inventory.dto.EquipmentSetOperationContract.DonationSetRequest;
import com.comandos.inventory.dto.EquipmentSetOperationContract.MaintenanceSetRequest;
import com.comandos.inventory.service.EquipmentSetOperationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/equipment-sets/operations")
public class EquipmentSetOperationController {
    private final EquipmentSetOperationService service;

    public EquipmentSetOperationController(EquipmentSetOperationService service) {
        this.service = service;
    }

    @PostMapping("/donation")
    @ResponseStatus(HttpStatus.CREATED)
    public AggregateOperationView donate(@RequestBody DonationSetRequest request) {
        return service.donate(request);
    }

    @PostMapping("/disposal")
    @ResponseStatus(HttpStatus.CREATED)
    public AggregateOperationView dispose(@RequestBody DisposalSetRequest request) {
        return service.dispose(request);
    }

    @PostMapping("/consumption")
    @ResponseStatus(HttpStatus.CREATED)
    public AggregateOperationView consume(@RequestBody ConsumableSetRequest request) {
        return service.consume(request);
    }

    @PostMapping("/maintenance")
    @ResponseStatus(HttpStatus.CREATED)
    public AggregateOperationView maintain(@RequestBody MaintenanceSetRequest request) {
        return service.maintain(request);
    }
}
