package com.comandos.inventory.controller;

import com.comandos.inventory.service.AmmunitionBoxService;
import com.comandos.inventory.service.AmmunitionStockIntakeService;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Ammunition routes complement the shared inventory API. */
@RestController
@RequestMapping("/api/erp/inventory")
public class AmmunitionInventoryController {
    private final AmmunitionStockIntakeService intake;
    private final AmmunitionBoxService ammunitionBoxes;

    public AmmunitionInventoryController(AmmunitionStockIntakeService intake, AmmunitionBoxService ammunitionBoxes) {
        this.intake = intake;
        this.ammunitionBoxes = ammunitionBoxes;
    }
    @PostMapping("/lots/from-boxes")
    public com.comandos.inventory.service.AmmunitionStockIntakeService.Receipt receiveBoxes(
            @RequestBody com.comandos.inventory.service.AmmunitionStockIntakeService.BoxesRequest request) {
        return intake.boxes(request);
    }

    @GetMapping("/ammunition-boxes")
    public AmmunitionBoxService.PageResult ammunitionBoxes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long organizationId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) String intakeRequestId,
            @RequestParam(required = false) String status) {
        return ammunitionBoxes.list(page, size, organizationId, lotId, intakeRequestId, status);
    }

    @GetMapping("/ammunition-boxes/{id}")
    public Map<String, Object> ammunitionBox(@PathVariable long id) {
        return ammunitionBoxes.get(id);
    }

}