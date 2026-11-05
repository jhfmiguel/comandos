package com.comandos.maintenance.service;
import com.fariamiguel.app.maintenance.service.MaintenanceOrigin;
import com.fariamiguel.app.maintenance.service.MaintenanceService;
import com.fariamiguel.app.maintenance.service.MaintenanceOperations;
import com.comandos.custody.model.CustodyReturnItem;
import com.fariamiguel.app.inventory.model.AssetItem;
import com.fariamiguel.app.maintenance.model.*;
import com.comandos.maintenance.model.*;
import com.fariamiguel.app.maintenance.dto.MaintenanceContract.*;
import com.comandos.maintenance.dto.SecurityMaintenanceContract.OpenRequest;
import com.comandos.maintenance.dto.SecurityMaintenanceContract.OrderView;
import jakarta.persistence.*;
import java.util.*;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Primary
@Transactional(readOnly=true)
public class ComandosMaintenanceService implements MaintenanceOperations<OpenRequest,OrderView>{
    private final EntityManager em;
    private final MaintenanceService core;
    private final CustodyMaintenanceViews views;
    public ComandosMaintenanceService(EntityManager em,MaintenanceService core,CustodyMaintenanceViews views){this.em=em;this.core=core;this.views=views;}
    public Class<OpenRequest> openRequestType(){return OpenRequest.class;}
    public PlanView plan(PlanRequest request){return core.plan(request);}
    public Page<PlanView> plans(long org,Long unit){return core.plans(org,unit);}
    public Page<AssetOption> assets(long org,Long unit,String search){return core.assets(org,unit,search);}
    @Transactional public OrderView open(OpenRequest r){
        if(r==null)return views.view(core.open(null));
        var generic=new com.fariamiguel.app.maintenance.dto.MaintenanceContract.OpenRequest(r.requestId(),r.organizationId(),r.unitId(),r.planId(),r.assetId(),r.reason(),r.maintenanceType(),r.workshop(),r.gunsmith());
        return views.view(core.open(generic,new CustodyOrigin(r.custodyReturnItemId())));
    }
    @Transactional public OrderView complete(long id,CompleteRequest request){return views.view(core.complete(id,request));}
    public Page<OrderView> orders(long org,Long unit,int page){
        var result=core.orders(org,unit,page);
        return new Page<>(result.content().stream().map(views::view).toList(),result.totalElements(),result.page(),result.size());
    }
    private final class CustodyOrigin implements MaintenanceOrigin{
        private final Long id;
        private CustodyReturnItem source;
        private CustodyOrigin(Long id){this.id=id;}
        public String fingerprintPart(){return String.valueOf(id);}
        public boolean hasReference(){return id!=null;}
        public void validate(AssetItem asset,long organizationId,Long unitId){
            if(id==null)return;
            source=em.find(CustodyReturnItem.class,id,LockModeType.PESSIMISTIC_WRITE);
            if(source==null)bad("CustodyReturnItem not found.");
            var item=source.custodyItem;
            if(item.asset==null||!item.asset.id.equals(asset.id))bad("Custody return item does not belong to the selected asset.");
            if(source.conditionType==null||!source.conditionType.blocksAvailability)conflict("Only a blocking custody return inspection can originate maintenance.");
            var custody=item.custody;
            if(!Objects.equals(custody.organizationLegacyId,organizationId)||!Objects.equals(custody.unitLegacyId,unitId))bad("Custody return item does not belong to the selected scope.");
            if(em.createQuery("select count(w) from WorkOrderCustodyDetails w where w.custodyReturnItem.id=:id",Long.class).setParameter("id",id).getSingleResult()>0)
                conflict("A work order already exists for this custody return item.");
        }
        public void attach(WorkOrder order){
            if(source==null)return;
            var details=em.find(WorkOrderCustodyDetails.class,order.id);
            if(details==null)throw new IllegalStateException("The persisted work order is missing.");
            details.custodyReturnItem=source;
        }
    }
    private static void bad(String reason){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,reason);}
    private static void conflict(String reason){throw new ResponseStatusException(HttpStatus.CONFLICT,reason);}
}