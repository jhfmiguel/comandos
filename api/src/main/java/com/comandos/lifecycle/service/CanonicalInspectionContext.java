package com.comandos.lifecycle.service;
import com.fariamiguel.app.lifecycle.service.InspectionContext;

import com.fariamiguel.app.lifecycle.model.PeriodicInspection;
import com.comandos.maintenance.dto.SecurityMaintenanceContract.OpenRequest;
import com.comandos.maintenance.service.ComandosMaintenanceService;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class CanonicalInspectionContext implements InspectionContext {
    private final ComandosMaintenanceService maintenance;
    public CanonicalInspectionContext(ComandosMaintenanceService maintenance) { this.maintenance = maintenance; }
    @Override
    public long organizationId(PeriodicInspection inspection) { return inspection.organizationLegacyId; }
    @Override
    public Long unitId(PeriodicInspection inspection) { return inspection.unitLegacyId; }
    @Override
    public long openCorrectiveMaintenance(PeriodicInspection inspection, String reason) {
        return maintenance.open(new OpenRequest(UUID.randomUUID().toString(), organizationId(inspection),
            unitId(inspection), null, inspection.asset.id, reason, null, "CORRECTIVE", null, null)).id();
    }
}