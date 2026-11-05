package com.comandos.maintenance.service;
import com.fariamiguel.app.maintenance.service.MaintenanceAuditContributor;
import com.comandos.maintenance.dto.SecurityMaintenanceContract.OrderView;
import com.fariamiguel.app.maintenance.model.*;
import com.comandos.maintenance.model.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class CustodyMaintenanceViews implements MaintenanceAuditContributor {
    private final EntityManager em;
    public CustodyMaintenanceViews(EntityManager em){this.em=em;}
    public Long sourceId(long id){
        var details=em.find(WorkOrderCustodyDetails.class,id);
        return details==null||details.custodyReturnItem==null?null:details.custodyReturnItem.id;
    }
    public OrderView view(com.fariamiguel.app.maintenance.dto.MaintenanceContract.OrderView value){
        return new OrderView(value.id(),value.organizationId(),value.organizationName(),value.unitId(),value.unitName(),value.planId(),value.planName(),value.assetId(),value.assetCode(),value.modelName(),value.locationName(),value.reason(),value.status(),value.openedAt(),value.completedAt(),value.openedByLogin(),value.completedByLogin(),value.defect(),value.cause(),value.opinion(),value.services(),value.testResult(),value.testNotes(),sourceId(value.id()),value.maintenanceType(),value.workshop(),value.gunsmith(),value.nextMaintenanceAt(),value.totalCost(),value.parts());
    }
    @Override
    public Map<String,Object> fields(WorkOrder order){
        var fields=new LinkedHashMap<String,Object>();
        fields.put("custodyReturnItemId",sourceId(order.id));
        return fields;
    }
}