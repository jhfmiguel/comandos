package com.comandos.consumption.controller;

import com.comandos.consumption.dto.AmmunitionConsumptionContract.*;
import com.comandos.consumption.service.AmmunitionConsumptionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/ammunition-consumptions")
public class AmmunitionConsumptionController {
    private final AmmunitionConsumptionService service;
    public AmmunitionConsumptionController(AmmunitionConsumptionService service) { this.service = service; }
    @GetMapping("/stock") public Page<StockOption> stock(@RequestParam long organizationId,
        @RequestParam(required = false) Long unitId, @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page) { return service.stock(organizationId, unitId, search, page); }
    @PostMapping public ConsumptionView finalize(@RequestBody FinalizeRequest request) { return service.finalize(request); }
    @GetMapping public Page<ConsumptionView> list(@RequestParam long organizationId,
        @RequestParam(required = false) Long unitId, @RequestParam(defaultValue = "0") int page) {
        return service.list(organizationId, unitId, page);
    }
    @GetMapping("/{id}") public ConsumptionView get(@PathVariable long id) { return service.get(id); }
}
