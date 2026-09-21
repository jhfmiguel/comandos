package com.weaponsregistration.custody.controller;

import com.weaponsregistration.custody.dto.CustodyContract.*;
import com.weaponsregistration.custody.service.CustodyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/custodies")
public class CustodyController {
    private final CustodyService service;
    public CustodyController(CustodyService service) { this.service = service; }
    @GetMapping("/stock")
    public Page<StockOption> stock(@RequestParam long organizationId, @RequestParam(required = false) Long unitId,
        @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page) {
        return service.stock(organizationId, unitId, search, page);
    }
    @GetMapping("/equipment-sets")
    public Page<EquipmentSetOption> equipmentSets(@RequestParam long organizationId, @RequestParam(required = false) Long unitId,
        @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page) {
        return service.equipmentSets(organizationId, unitId, search, page);
    }
    @PostMapping public CustodyView issue(@RequestBody IssueRequest request) { return service.issue(request); }
    @PostMapping("/{id}/returns") public CustodyView returnItems(@PathVariable long id, @RequestBody ReturnRequest request) {
        return service.returnItems(id, request);
    }
    @GetMapping public Page<CustodyView> list(@RequestParam long organizationId, @RequestParam(required = false) Long unitId,
        @RequestParam(defaultValue = "0") int page) { return service.list(organizationId, unitId, page); }
    @GetMapping("/{id}") public CustodyView get(@PathVariable long id) { return service.get(id); }
    @GetMapping("/by-asset/{assetId}") public Page<CustodyView> byAsset(@PathVariable long assetId,
        @RequestParam(defaultValue = "0") int page) { return service.byAsset(assetId, page); }
}
