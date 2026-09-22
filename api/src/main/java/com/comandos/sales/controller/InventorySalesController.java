package com.comandos.sales.controller;

import com.comandos.sales.dto.SalesContract.*;
import com.comandos.sales.service.InventorySalesService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/sales")
public class InventorySalesController {
    private final InventorySalesService service;
    public InventorySalesController(InventorySalesService service) { this.service = service; }
    @GetMapping("/stock")
    public Page<StockOption> stock(@RequestParam long organizationId, @RequestParam(required = false) Long unitId, @RequestParam String kind,
            @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page) {
        return service.stock(organizationId, unitId, kind, search, page);
    }
    @PostMapping
    public SaleView finalizeSale(@RequestBody FinalizeRequest request) { return service.finalizeSale(request); }
    @GetMapping
    public Page<SaleView> list(@RequestParam long organizationId, @RequestParam(required = false) Long unitId, @RequestParam(defaultValue = "0") int page) {
        return service.list(organizationId, unitId, page);
    }
    @GetMapping("/{id}")
    public SaleView get(@PathVariable long id) { return service.get(id); }
    @PostMapping("/{id}/returns")
    public ReturnView returnItems(@PathVariable long id, @RequestBody ReturnRequest request) { return service.returnItems(id, request); }
}
