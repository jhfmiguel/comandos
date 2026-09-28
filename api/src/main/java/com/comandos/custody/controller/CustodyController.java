package com.comandos.custody.controller;

import com.comandos.custody.dto.CustodyContract.*;
import com.comandos.custody.service.CustodyIssueFacade;
import com.comandos.custody.service.CustodyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/custodies")
public class CustodyController {
    private final CustodyService service;
    private final CustodyIssueFacade issueFacade;

    public CustodyController(CustodyService service, CustodyIssueFacade issueFacade) {
        this.service = service;
        this.issueFacade = issueFacade;
    }

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

    @PostMapping
    public CustodyView issue(@RequestBody IssueRequest request) {
        return issueFacade.issueIndividual(request);
    }

    @PostMapping("/institutional")
    public CustodyView issueInstitutional(@RequestBody InstitutionalIssueRequest request) {
        return issueFacade.issueInstitutional(request);
    }

    @GetMapping("/{id}/responsibility")
    public CustodyResponsibilityView responsibility(@PathVariable long id) {
        return issueFacade.responsibility(id);
    }

    @PostMapping("/{id}/returns")
    public CustodyView returnItems(@PathVariable long id, @RequestBody ReturnRequest request) {
        return service.returnItems(id, request);
    }

    @GetMapping
    public Page<CustodyView> list(@RequestParam long organizationId, @RequestParam(required = false) Long unitId,
        @RequestParam(defaultValue = "0") int page) {
        return service.list(organizationId, unitId, page);
    }

    @GetMapping("/{id}")
    public CustodyView get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping("/by-asset/{assetId}")
    public Page<CustodyView> byAsset(@PathVariable long assetId,
        @RequestParam(defaultValue = "0") int page) {
        return service.byAsset(assetId, page);
    }
}
