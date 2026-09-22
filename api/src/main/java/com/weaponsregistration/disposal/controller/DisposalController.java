package com.weaponsregistration.disposal.controller;

import com.weaponsregistration.disposal.dto.DisposalContract.DisposalView;
import com.weaponsregistration.disposal.dto.DisposalContract.FinalizeRequest;
import com.weaponsregistration.disposal.dto.DisposalContract.Page;
import com.weaponsregistration.disposal.dto.DisposalContract.StockOption;
import com.weaponsregistration.disposal.service.DisposalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/erp/disposals")
public class DisposalController {
    private final DisposalService service;
    public DisposalController(DisposalService service) { this.service = service; }
    @GetMapping("/stock") public Page<StockOption> stock(@RequestParam long organizationId,
        @RequestParam(required = false) Long unitId, @RequestParam String kind,
        @RequestParam(defaultValue = "") String search, @RequestParam(defaultValue = "0") int page) {
        return service.stock(organizationId, unitId, kind, search, page);
    }
    @PostMapping public DisposalView finalize(@RequestBody FinalizeRequest request) { return service.finalize(request); }
    @GetMapping public Page<DisposalView> list(@RequestParam long organizationId, @RequestParam(required = false) Long unitId,
        @RequestParam(defaultValue = "0") int page) { return service.list(organizationId, unitId, page); }
    @GetMapping("/{id}") public DisposalView get(@PathVariable long id) { return service.get(id); }
}
