package com.comandos.transfer.controller;

import com.comandos.transfer.dto.TransferContract.*;
import com.comandos.transfer.service.TransferService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/transfers")
public class TransferController {
    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @GetMapping("/stock")
    public Page<StockOption> stock(
            @RequestParam long organizationId,
            @RequestParam long sourceUnitId,
            @RequestParam String kind,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page) {
        return service.stock(organizationId, sourceUnitId, kind, search, page);
    }

    /** Legacy creation endpoint retained for existing clients. Semantically this dispatches the stock. */
    @PostMapping
    public TransferView finalize(@RequestBody FinalizeRequest request) {
        return service.finalize(request);
    }

    /** Explicit logistics action: stock leaves the source and enters IN_TRANSIT. */
    @PostMapping("/dispatch")
    public TransferView dispatch(@RequestBody FinalizeRequest request) {
        return service.finalize(request);
    }

    @GetMapping
    public Page<TransferView> list(
            @RequestParam long organizationId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(defaultValue = "0") int page) {
        return service.list(organizationId, unitId, page);
    }

    @GetMapping("/{id}")
    public TransferView get(@PathVariable long id) {
        return service.get(id);
    }

    /** Legacy acceptance endpoint retained for compatibility. */
    @PostMapping("/{id}/accept")
    public TransferView accept(@PathVariable long id, @RequestBody AcceptRequest request) {
        return service.accept(id, request);
    }

    /** Explicit logistics action: destination confirms physical receipt. */
    @PostMapping("/{id}/receive")
    public TransferView receive(@PathVariable long id, @RequestBody AcceptRequest request) {
        return service.accept(id, request);
    }

    @PostMapping("/{id}/reject")
    public TransferView reject(@PathVariable long id, @RequestBody RejectRequest request) {
        return service.reject(id, request);
    }
}
