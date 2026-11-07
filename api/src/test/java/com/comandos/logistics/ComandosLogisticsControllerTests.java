package com.comandos.logistics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.comandos.security.service.ComandosAccessPolicy;
import com.fariamiguel.enterprise.logistics.InMemoryLogisticsDirectory;
import com.fariamiguel.enterprise.logistics.LogisticsDirectory;
import com.fariamiguel.enterprise.logistics.LogisticsService;
import com.fariamiguel.security.api.CurrentActor;
import com.fariamiguel.security.api.CurrentActorProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ComandosLogisticsControllerTests {
    private ComandosAccessPolicy access;
    private LogisticsService service;
    private ComandosLogisticsController controller;

    @BeforeEach void setUp(){
        access=mock(ComandosAccessPolicy.class);
        service=new LogisticsService(new InMemoryLogisticsDirectory());
        CurrentActorProvider actors=()->new CurrentActor("authenticated-user","User",Set.of(),Set.of(),Set.of(),"session",true);
        controller=new ComandosLogisticsController(service,access,actors);
    }

    @Test void warehouseBinIsTenantOwnedAndOrganizationUnitAreServerScoped(){
        var bin=controller.saveBin(new ComandosLogisticsController.BinRequest("bin-a",44,3L,"warehouse-a",null,"A-01","Aisle 1","EA",null));
        verify(access).requireScope("inventory/logistics","CREATE",44L,3L);
        assertEquals("comandos",bin.tenant());
        assertEquals(Map.of("organizationId","44","unitId","3"),bin.metadata());
        assertEquals(1,controller.bins(44,3L,null).size());
        assertTrue(controller.bins(44,4L,null).isEmpty());
    }

    @Test void deniedOrganizationCannotCreateBin(){
        doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
                .when(access).requireScope("inventory/logistics","CREATE",99L,null);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.saveBin(
                new ComandosLogisticsController.BinRequest("bin-x",99,null,"warehouse-a",null,"X-01","Forbidden","EA",null)));
        assertTrue(service.warehouseBins("comandos",null).isEmpty());
    }

    @Test void transferMustStayWithinSelectedScopeAndUsesAuthenticatedActor(){
        controller.saveBin(new ComandosLogisticsController.BinRequest("from",44,3L,"warehouse-a",null,"A-01","A","EA",null));
        controller.saveBin(new ComandosLogisticsController.BinRequest("to",44,3L,"warehouse-a",null,"B-01","B","EA",null));
        controller.saveBin(new ComandosLogisticsController.BinRequest("other",45,3L,"warehouse-b",null,"C-01","C","EA",null));
        controller.receive("from",44,3L,new ComandosLogisticsController.ReceiptRequest("receipt","sku",null,new BigDecimal("5"),"EA",Instant.now(),"PURCHASE","p1",new BigDecimal("12.50"),"BRL"));
        var transfer=controller.transfer(new ComandosLogisticsController.TransferRequest("transfer",44,3L,"sku",null,"from","to",new BigDecimal("2"),"EA",Instant.now(),"REQUEST","r1"));
        assertEquals("authenticated-user",transfer.actor());
        assertEquals(new BigDecimal("3"),service.inventoryBalance("comandos","sku",null,"from","EA").onHand());
        assertEquals(new BigDecimal("2"),service.inventoryBalance("comandos","sku",null,"to","EA").onHand());
        assertEquals(1,controller.transfers(44,3L,"sku").size());
        assertTrue(controller.transfers(45,3L,"sku").isEmpty());
        assertEquals("authenticated-user",controller.movements("from",44,3L,"sku").get(0).actor());
        assertEquals("BRL",controller.movements("from",44,3L,"sku").get(0).currency());
        assertEquals(new BigDecimal("12.500000000000"),controller.valuation("from",44,3L,"sku",null,"EA",LogisticsDirectory.InventoryCostingMethod.WEIGHTED_AVERAGE).averageUnitCost());
        var adjustment=controller.adjust("from",44,3L,new ComandosLogisticsController.AdjustmentRequest("count-1","sku",null,LogisticsDirectory.InventoryMovementType.ADJUSTMENT_OUT,BigDecimal.ONE,"EA",Instant.now(),"count-sheet-1",null,null));
        assertEquals("authenticated-user",adjustment.actor());
        assertEquals(new BigDecimal("2"),service.inventoryBalance("comandos","sku",null,"from","EA").onHand());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.transfer(
                new ComandosLogisticsController.TransferRequest("cross",44,3L,"sku",null,"from","other",BigDecimal.ONE,"EA",Instant.now(),null,null)));
    }

    @Test void shipmentEventsAndInternationalImportStayInsideOrganizationScope(){
        controller.saveBin(new ComandosLogisticsController.BinRequest("origin",44,3L,"warehouse-a",null,"A-01","A","EA",null));
        controller.saveBin(new ComandosLogisticsController.BinRequest("destination",44,3L,"warehouse-a",null,"B-01","B","EA",null));
        controller.saveBin(new ComandosLogisticsController.BinRequest("foreign",45,3L,"warehouse-b",null,"X-01","X","EA",null));
        var shipment=controller.createShipment(new ComandosLogisticsController.ShipmentRequest("ship-1",44,3L,"S-001","origin","destination","Carrier","track-1",Instant.now(),"Receiver",null));
        controller.addShipmentItem(shipment.id(),44,3L,new ComandosLogisticsController.ShipmentItemRequest("line-1","sku",null,null,BigDecimal.ONE,"EA",Map.of()));
        var event=controller.recordShipmentEvent(shipment.id(),44,3L,new ComandosLogisticsController.ShipmentEventRequest("pick-1",LogisticsDirectory.ShipmentEventType.PICKED,Instant.now(),"origin","Picked",null,Map.of()));
        assertEquals("authenticated-user",event.actor());
        assertEquals(1,controller.shipments(44,3L).size());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.recordShipmentEvent(shipment.id(),45,3L,new ComandosLogisticsController.ShipmentEventRequest("bad",LogisticsDirectory.ShipmentEventType.OCCURRENCE,Instant.now(),"foreign","outside scope",null,Map.of())));
        var imported=controller.createImport(new ComandosLogisticsController.ImportRequest("imp-1",44,3L,"IMP-01","supplier-1","US","PO-1","USD",new BigDecimal("5.00"),null,java.time.LocalDate.parse("2026-11-01"),null,"destination",null));
        var expense=controller.addImportExpense(imported.id(),44,3L,new ComandosLogisticsController.ImportExpenseRequest("expense-1","FREIGHT",new BigDecimal("10.00"),"USD",new BigDecimal("5.00"),new BigDecimal("50.00"),java.time.LocalDate.parse("2026-11-02"),"invoice:1",null,Map.of()));
        assertEquals(new BigDecimal("50.00"),expense.baseCurrencyAmount());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.addImportExpense(imported.id(),45,3L,new ComandosLogisticsController.ImportExpenseRequest("expense-2","FREIGHT",BigDecimal.ONE,"USD",new BigDecimal("5"),new BigDecimal("5"),java.time.LocalDate.parse("2026-11-02"),null,null,Map.of())));
    }

    @Test void reservationLifecycleAndImportStatusHistoryKeepAuthenticatedActor(){
        controller.saveBin(new ComandosLogisticsController.BinRequest("bin",44,3L,"warehouse",null,"A-01","A","EA",null));
        controller.registerLot(new ComandosLogisticsController.LotRequest("lot",44,3L,"sku","LOT-1",null,java.time.LocalDate.now().plusDays(30),"supplier","purchase",LogisticsDirectory.LotStatus.ACTIVE,Instant.now()));
        controller.receive("bin",44,3L,new ComandosLogisticsController.ReceiptRequest("receipt","sku","lot",new BigDecimal("4"),"EA",Instant.now(),"PURCHASE","po-1",new BigDecimal("2.50"),"USD"));
        var reservation=controller.reserve("bin",44,3L,new ComandosLogisticsController.ReservationRequest("reservation","sku","lot",BigDecimal.ONE,"EA","ORDER","order-1",Instant.now(),null));
        assertEquals("authenticated-user",reservation.actor());
        assertEquals(LogisticsDirectory.ReservationStatus.RELEASED,controller.releaseReservation("reservation",44,3L).status());
        var imported=controller.createImport(new ComandosLogisticsController.ImportRequest("imp-history",44,3L,"IMP-H","supplier-1","US","PO-2","USD",BigDecimal.ONE,null,java.time.LocalDate.now(),null,"bin",null));
        controller.updateImportStatus(imported.id(),44,3L,new ComandosLogisticsController.ImportStatusRequest(LogisticsDirectory.ImportStatus.IN_TRANSIT,null,null,null,"Dispatched"));
        var history=controller.importHistory(imported.id(),44,3L);
        assertEquals(1,history.size());
        assertEquals(LogisticsDirectory.ImportStatus.PLANNED,history.get(0).from());
        assertEquals(LogisticsDirectory.ImportStatus.IN_TRANSIT,history.get(0).to());
        assertEquals("authenticated-user",history.get(0).actor());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.importHistory(imported.id(),45,3L));
        assertEquals(LogisticsDirectory.LotStatus.QUARANTINED,controller.updateLotStatus("lot",44,3L,new ComandosLogisticsController.LotStatusRequest(LogisticsDirectory.LotStatus.QUARANTINED)).status());
        assertEquals("authenticated-user",service.inventoryLots("comandos","sku").get(0).metadata().get("statusChangedBy"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.updateLotStatus("lot",45,3L,new ComandosLogisticsController.LotStatusRequest(LogisticsDirectory.LotStatus.ACTIVE)));
    }

    @Test void importLineLandedCostIsAllocatedAndReceiptIsPostedToScopedInventory(){
        controller.saveBin(new ComandosLogisticsController.BinRequest("import-bin",44,3L,"warehouse",null,"IMP-01","Import receiving","EA",null));
        var imported=controller.createImport(new ComandosLogisticsController.ImportRequest("imp-line",44,3L,"IMP-LINE","supplier-1","US","PO-LINE","USD",new BigDecimal("5"),null,java.time.LocalDate.parse("2026-11-01"),null,"import-bin",null));
        controller.updateImportStatus(imported.id(),44,3L,new ComandosLogisticsController.ImportStatusRequest(LogisticsDirectory.ImportStatus.IN_TRANSIT,null,null,null,"Dispatched"));
        controller.updateImportStatus(imported.id(),44,3L,new ComandosLogisticsController.ImportStatusRequest(LogisticsDirectory.ImportStatus.ARRIVED,java.time.LocalDate.parse("2026-11-04"),null,null,"Arrived"));
        controller.updateImportStatus(imported.id(),44,3L,new ComandosLogisticsController.ImportStatusRequest(LogisticsDirectory.ImportStatus.CLEARED,null,java.time.LocalDate.parse("2026-11-05"),"customs-1","Cleared"));
        var line=controller.addImportLine(imported.id(),44,3L,new ComandosLogisticsController.ImportLineRequest("line-1","sku-import",null,new BigDecimal("2"),"EA",new BigDecimal("10"),"USD",new BigDecimal("5"),new BigDecimal("50"),"BRL","supplier-line-1"));
        controller.addImportExpense(imported.id(),44,3L,new ComandosLogisticsController.ImportExpenseRequest("expense-import","FREIGHT",new BigDecimal("10"),"USD",new BigDecimal("5"),new BigDecimal("50"),java.time.LocalDate.parse("2026-11-05"),"ged:freight",null,Map.of("baseCurrency","BRL")));
        var cost=controller.landedCost(imported.id(),line.id(),44,3L);
        assertEquals(new BigDecimal("75.00000000"),cost.landedUnitCost());
        var receipt=controller.receiveImportLine(imported.id(),line.id(),44,3L,new ComandosLogisticsController.ImportLineReceiptRequest("import-receipt",BigDecimal.ONE,Instant.now()));
        assertEquals(new BigDecimal("75.00000000"),receipt.unitCost());
        assertEquals("authenticated-user",receipt.actor());
        assertEquals("44",receipt.metadata().get("organizationId"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->controller.landedCost(imported.id(),line.id(),45,3L));
        assertThrows(IllegalArgumentException.class,()->controller.updateImportStatus(imported.id(),44,3L,new ComandosLogisticsController.ImportStatusRequest(LogisticsDirectory.ImportStatus.RECEIVED,null,null,null,"premature")));
        controller.receiveImportLine(imported.id(),line.id(),44,3L,new ComandosLogisticsController.ImportLineReceiptRequest("import-receipt-2",BigDecimal.ONE,Instant.now().plusSeconds(1)));
        assertEquals(LogisticsDirectory.ImportStatus.RECEIVED,controller.updateImportStatus(imported.id(),44,3L,new ComandosLogisticsController.ImportStatusRequest(LogisticsDirectory.ImportStatus.RECEIVED,null,null,null,"All lines received")).status());
    }
}

