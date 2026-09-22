package com.comandos.inventory.controller;

import com.comandos.inventory.service.InventoryCatalog;
import com.comandos.inventory.service.InventoryService;
import com.comandos.security.service.AccessPolicy;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/inventory")
public class InventoryController {
    private final InventoryService service;
    private final AccessPolicy access;
    private final com.comandos.inventory.service.StockIntakeService intake;
    public InventoryController(InventoryService service, AccessPolicy access, com.comandos.inventory.service.StockIntakeService intake) {
        this.service = service; this.access = access; this.intake = intake;
    }

    @PostMapping("/assets/batch")
    public org.springframework.http.ResponseEntity<Map<String, Object>> createAssets(
            @RequestBody com.comandos.inventory.service.StockIntakeService.AssetsRequest request) {
        return batchResponse(intake.assets(request));
    }

    @PostMapping("/assets/batch/review")
    public org.springframework.http.ResponseEntity<Map<String, Object>> reviewAssets(
            @RequestBody com.comandos.inventory.service.StockIntakeService.AssetsRequest request) {
        return batchResponse(intake.reviewAssets(request));
    }

    private org.springframework.http.ResponseEntity<Map<String, Object>> batchResponse(
            com.comandos.inventory.service.StockIntakeService.AssetResult result) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("accepted", result.accepted()); body.put("detail", result.detail()); body.put("rows", result.rows());
        body.put("quantity", result.receipt() == null ? "0" : result.receipt().quantity());
        body.put("recordIds", result.receipt() == null ? List.of() : result.receipt().recordIds());
        if (result.receipt() != null) body.put("id", result.receipt().id());
        return org.springframework.http.ResponseEntity.status(result.accepted() ? 200 : 400).body(body);
    }

    @PostMapping("/lots/from-boxes")
    public com.comandos.inventory.service.StockIntakeService.Receipt receiveBoxes(
            @RequestBody com.comandos.inventory.service.StockIntakeService.BoxesRequest request) {
        return intake.boxes(request);
    }

    @GetMapping("/catalog")
    public List<Map<String, Object>> catalog() {
        return InventoryCatalog.RESOURCES.stream().filter(r -> access.canAny("inventory/" + r.key(), "READ")).map(r -> Map.<String, Object>of(
            "key", r.key(), "label", r.label(), "group", r.group(), "fields", r.fields(), "readOnly", r.readOnly(),
            "actions", r.readOnly() ? List.of("READ") : access.actions("inventory/" + r.key()))).toList();
    }

    @GetMapping("/{resource}")
    public InventoryService.PageResult list(@PathVariable String resource,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long organizationId,
            @RequestParam Map<String, String> params) {
        return service.list(resource, search, page, size, organizationId, params);
    }

    @GetMapping("/{resource}/{id}")
    public Map<String, Object> get(@PathVariable String resource, @PathVariable long id) {
        return service.get(resource, id);
    }

    @PostMapping("/{resource}") @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@PathVariable String resource, @RequestBody Map<String, Object> data) {
        return service.save(resource, null, data);
    }

    @PutMapping("/{resource}/{id}")
    public Map<String, Object> update(@PathVariable String resource, @PathVariable long id,
                                    @RequestBody Map<String, Object> data) {
        return service.save(resource, id, data);
    }

    @DeleteMapping("/{resource}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String resource, @PathVariable long id, @RequestParam Long version) {
        service.delete(resource, id, version);
    }
}
