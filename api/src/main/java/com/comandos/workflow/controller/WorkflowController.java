package com.comandos.workflow.controller;

import com.comandos.workflow.api.*;
import com.comandos.workflow.service.WorkflowService;
import com.fariamiguel.core.api.PlatformPage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/erp/workflows")
public class WorkflowController {
    private final WorkflowService service;

    public WorkflowController(WorkflowService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowView request(@RequestBody WorkflowRequest request) {
        return service.request(request);
    }

    @GetMapping("/{id}")
    public WorkflowView get(@PathVariable long id) {
        return service.get(id);
    }

    @GetMapping
    public PlatformPage<WorkflowView> list(
            @RequestParam long organizationId,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page) {
        return service.list(organizationId, unitId, status, page);
    }

    @PostMapping("/{id}/analyze")
    public WorkflowView analyze(@PathVariable long id, @RequestBody(required = false) WorkflowTransition request) {
        return service.analyze(id, request);
    }

    @PostMapping("/{id}/authorize")
    public WorkflowView authorize(@PathVariable long id, @RequestBody(required = false) WorkflowTransition request) {
        return service.authorize(id, request);
    }

    @PostMapping("/{id}/execute")
    public WorkflowView execute(@PathVariable long id, @RequestBody(required = false) WorkflowTransition request) {
        return service.execute(id, request);
    }

    @PostMapping("/{id}/conclude")
    public WorkflowView conclude(@PathVariable long id, @RequestBody(required = false) WorkflowTransition request) {
        return service.conclude(id, request);
    }

    @PostMapping("/{id}/cancel")
    public WorkflowView cancel(@PathVariable long id, @RequestBody(required = false) WorkflowTransition request) {
        return service.cancel(id, request);
    }
}
