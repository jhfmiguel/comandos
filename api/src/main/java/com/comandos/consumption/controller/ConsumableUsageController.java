package com.comandos.consumption.controller;

import com.comandos.consumption.dto.ConsumableUsageContract.*;
import com.comandos.consumption.service.ConsumableUsageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/consumable-usages")
public class ConsumableUsageController {
    private final ConsumableUsageService service;
    public ConsumableUsageController(ConsumableUsageService service) { this.service = service; }

    @GetMapping("/stock")
    public Page<StockOption> stock(@RequestParam long organizationId,
                                  @RequestParam(required = false) Long unitId,
                                  @RequestParam(defaultValue = "") String search,
                                  @RequestParam(defaultValue = "0") int page) {
        return service.stock(organizationId, unitId, search, page);
    }

    @PostMapping
    public UsageView finalizeUsage(@RequestBody FinalizeRequest request) {
        return service.finalizeUsage(request);
    }

    @GetMapping
    public Page<UsageView> list(@RequestParam long organizationId,
                                @RequestParam(required = false) Long unitId,
                                @RequestParam(defaultValue = "0") int page) {
        return service.list(organizationId, unitId, page);
    }

    @GetMapping("/{id}")
    public UsageView get(@PathVariable long id) { return service.get(id); }
}
