package com.comandos.maintenance.dto;
import com.fariamiguel.app.maintenance.dto.MaintenanceContract;

import java.util.List;
import com.fariamiguel.app.maintenance.dto.MaintenanceContract.*;

/** Maintenance contracts with a custody return inspection as origin. */
public final class SecurityMaintenanceContract {
    private SecurityMaintenanceContract() {}
    public record OpenRequest(String requestId,Long organizationId,Long unitId,Long planId,Long assetId,String reason,Long custodyReturnItemId,String maintenanceType,String workshop,String gunsmith){}
    public record OrderView(long id,long organizationId,String organizationName,Long unitId,String unitName,Long planId,String planName,long assetId,String assetCode,String modelName,String locationName,String reason,String status,String openedAt,String completedAt,String openedByLogin,String completedByLogin,String defect,String cause,String opinion,List<ServiceView>services,String testResult,String testNotes,Long custodyReturnItemId,String maintenanceType,String workshop,String gunsmith,String nextMaintenanceAt,String totalCost,List<PartView>parts){}
}
