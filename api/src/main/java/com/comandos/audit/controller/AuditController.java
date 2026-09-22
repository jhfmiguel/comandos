package com.comandos.audit.controller;

import com.comandos.audit.service.AuditService;
import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/audit")
public class AuditController {
    private final AuditService service;
    public AuditController(AuditService service) { this.service = service; }
    @GetMapping
    public AuditService.Page list(@RequestParam(required = false) String resource, @RequestParam(required = false) Long recordId,
            @RequestParam(required = false) String action, @RequestParam(required = false) String actor,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant until,
            @RequestParam(required = false) Long assetId, @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) Long actorId, @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(defaultValue = "0") int page) {
        return service.list(resource, recordId, action, actor, from, until, page, assetId, lotId, actorId, organizationId, unitId);
    }
    @GetMapping("/{id}") public AuditService.Detail get(@PathVariable long id) { return service.get(id); }
}
