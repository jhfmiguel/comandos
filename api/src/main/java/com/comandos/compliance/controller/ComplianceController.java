package com.comandos.compliance.controller;

import com.comandos.compliance.dto.ComplianceContract.PolicyRequest;
import com.comandos.compliance.dto.ComplianceContract.PolicyView;
import com.comandos.compliance.dto.ComplianceContract.SummaryView;
import com.comandos.compliance.service.ComplianceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/compliance")
public class ComplianceController {
    private final ComplianceService service;

    public ComplianceController(ComplianceService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public SummaryView summary(@RequestParam long organizationId, @RequestParam(required = false) Long unitId) {
        return service.summary(organizationId, unitId);
    }

    @GetMapping("/policy")
    public PolicyView policy(@RequestParam long organizationId, @RequestParam(required = false) Long unitId) {
        return service.policy(organizationId, unitId);
    }

    @PutMapping("/policy")
    public PolicyView policy(@RequestParam long organizationId, @RequestParam(required = false) Long unitId,
                             @RequestBody PolicyRequest request) {
        return service.savePolicy(organizationId, unitId, request);
    }
}
