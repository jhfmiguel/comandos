package com.comandos.reconciliation.controller;
import static org.junit.jupiter.api.Assertions.assertEquals;import java.math.*;import java.net.*;import java.net.http.*;import java.util.*;import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.web.server.LocalServerPort;import org.springframework.jdbc.core.JdbcTemplate;import tools.jackson.databind.*;import tools.jackson.databind.json.JsonMapper;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:inventory-count-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","logging.level.root=WARN","debug=false"})class InventoryCountApiTests{@LocalServerPort int port;@Autowired JdbcTemplate jdbc;final HttpClient client=HttpClient.newHttpClient();final JsonMapper json=JsonMapper.builder().build();record R(int status,JsonNode body,String raw){}record S(long org,long unit,long location,long asset,long lot,long balance){}
 @Test void approvesShortageAndSurplusWithAuditableAdjustments()throws Exception{var s=setup();var opened=request("POST","inventory-counts",Map.of("requestId",uuid(),"organizationId",s.org(),"unitId",s.unit(),"locationId",s.location(),"purpose","Annual physical verification"));assertEquals(200,opened.status(),opened.raw());long id=opened.body().get("id").asLong();assertEquals("OPEN",opened.body().get("statusCode").asText());assertEquals(2,opened.body().get("items").size());var lines=new ArrayList<Map<String,Object>>();for(var i:opened.body().get("items")){boolean asset="ASSET".equals(i.get("kind").asText());lines.add(Map.of("itemId",i.get("id").asLong(),"countedQuantity",asset?0:12,"notes",asset?"Not found during count":"Two units found"));}var counted=request("PUT","inventory-counts/"+id+"/count",Map.of("items",lines));assertEquals("COUNTED",counted.body().get("statusCode").asText());var approved=request("POST","inventory-counts/"+id+"/approve",null);assertEquals(200,approved.status(),approved.raw());assertEquals("APPROVED",approved.body().get("statusCode").asText());
        com.comandos.audit.controller.AuditTraceAssertions.trace(port,"inventory-counts",id,"assetId="+s.asset()+"&lotId="+s.lot()+"&unitId="+s.unit(),"OPEN","COUNT","APPROVE");assertEquals("BLOCKED",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));assertEquals("12.0000",decimal("select available from erp_stock_balance where id=?",s.balance()));assertEquals("12.0000",decimal("select available_quantity from erp_stock_lot where id=?",s.lot()));assertEquals(2,jdbc.queryForObject("select count(*) from erp_stock_movement where nature='INVENTORY_ADJUSTMENT' and location_id=?",Integer.class,s.location()));assertEquals(409,request("POST","maintenance/orders",Map.of("requestId",uuid(),"organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"reason","Inspect missing item")).status());}
 @Test void preventsTwoUnfinishedCountsAtTheSameLocation()throws Exception{var s=setup();var body=Map.of("requestId",uuid(),"organizationId",s.org(),"unitId",s.unit(),"locationId",s.location(),"purpose","First count");assertEquals(200,request("POST","inventory-counts",body).status());var second=Map.of("requestId",uuid(),"organizationId",s.org(),"unitId",s.unit(),"locationId",s.location(),"purpose","Second count");assertEquals(409,request("POST","inventory-counts",second).status());}
 @Test void individualCountsMustBeZeroOrOne() throws Exception {
     var s = setup(); var opened = open(s); long id = opened.get("id").asLong();
     var result = request("PUT", "inventory-counts/" + id + "/count", counts(opened, "0.5", "8"));
     assertEquals(400, result.status(), result.raw());
     assertEquals(0, jdbc.queryForObject("select count(*) from erp_inventory_count_item where inventory_count_id=? and counted_quantity is not null", Integer.class, id));
     assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id=?", String.class, s.asset()));
 }

 @Test void staleAssetShortageCannotBlockTransferredOrSoldEquipment() throws Exception {
     for (boolean sold : List.of(false, true)) {
         var s = setup(); var opened = open(s); long id = opened.get("id").asLong();
         assertEquals(200, request("PUT", "inventory-counts/" + id + "/count", counts(opened, "0", "8")).status());
         long expectedLocation = s.location();
         if (sold) {
             long buyer = create("core/people", Map.of("personType", "INDIVIDUAL", "fullName", "Buyer", "active", true));
             var sale = request("POST", "sales", Map.of("requestId", uuid(), "organizationId", s.org(), "unitId", s.unit(), "buyerId", buyer,
                 "paymentMethod", "PIX", "items", List.of(Map.of("assetId", s.asset(), "quantity", 1, "expectedUnitPrice", "1"))));
             assertEquals(200, sale.status(), sale.raw());
         } else {
             long destinationUnit = create("core/units", Map.of("organizationId", s.org(), "code", uuid(), "name", "Destination", "type", "Unit"));
             expectedLocation = create("inventory/locations", Map.of("organizationId", s.org(), "unitId", destinationUnit, "name", "Destination", "type", "Warehouse", "controlled", true));
             var transfer = request("POST", "transfers", Map.of("requestId", uuid(), "organizationId", s.org(), "sourceUnitId", s.unit(),
                 "destinationUnitId", destinationUnit, "destinationLocationId", expectedLocation, "purpose", "Move after count",
                 "items", List.of(Map.of("assetId", s.asset(), "quantity", 1))));
             assertEquals(200, transfer.status(), transfer.raw());
         }
         var approval = request("POST", "inventory-counts/" + id + "/approve", null);
         assertEquals(409, approval.status(), approval.raw());
         assertEquals(sold ? "SOLD" : "AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id=?", String.class, s.asset()));
         assertEquals(expectedLocation, jdbc.queryForObject("select location_id from erp_asset_item where id=?", Long.class, s.asset()));
         assertEquals("10.0000", decimal("select available from erp_stock_balance where id=?", s.balance()));
         assertEquals(0, jdbc.queryForObject("select count(*) from erp_stock_movement where nature='INVENTORY_ADJUSTMENT' and location_id=?", Integer.class, s.location()));
     }
 }

 @Test void lotMovementAfterSnapshotPreventsStaleAdjustment() throws Exception {
     var s = setup(); var opened = open(s); long id = opened.get("id").asLong();
     assertEquals(200, request("PUT", "inventory-counts/" + id + "/count", counts(opened, "0", "9")).status());
     long destinationUnit = create("core/units", Map.of("organizationId", s.org(), "code", uuid(), "name", "Destination", "type", "Unit"));
     long destination = create("inventory/locations", Map.of("organizationId", s.org(), "unitId", destinationUnit, "name", "Destination", "type", "Warehouse", "controlled", true));
     var transfer = request("POST", "transfers", Map.of("requestId", uuid(), "organizationId", s.org(), "sourceUnitId", s.unit(),
         "destinationUnitId", destinationUnit, "destinationLocationId", destination, "purpose", "Move after count",
         "items", List.of(Map.of("balanceId", s.balance(), "quantity", 2))));
     assertEquals(200, transfer.status(), transfer.raw());
     var approval = request("POST", "inventory-counts/" + id + "/approve", null);
     assertEquals(409, approval.status(), approval.raw());
     assertEquals("8.0000", decimal("select available from erp_stock_balance where id=?", s.balance()));
     assertEquals("10.0000", decimal("select available_quantity from erp_stock_lot where id=?", s.lot()));
     assertEquals("AVAILABLE", jdbc.queryForObject("select status from erp_asset_item where id=?", String.class, s.asset()));
     assertEquals(0, jdbc.queryForObject("select count(*) from erp_stock_movement where nature='INVENTORY_ADJUSTMENT' and location_id=?", Integer.class, s.location()));
 }

 private JsonNode open(S s) throws Exception {
     var response = request("POST", "inventory-counts", Map.of("requestId", uuid(), "organizationId", s.org(), "unitId", s.unit(), "locationId", s.location(), "purpose", "Regression count"));
     assertEquals(200, response.status(), response.raw()); return response.body();
 }
 private Map<String, Object> counts(JsonNode opened, String assetQuantity, String lotQuantity) {
     var lines = new ArrayList<Map<String, Object>>();
     for (var item : opened.get("items")) lines.add(Map.of("itemId", item.get("id").asLong(), "countedQuantity", "ASSET".equals(item.get("kind").asText()) ? assetQuantity : lotQuantity));
     return Map.of("items", lines);
 }
 private S setup()throws Exception{long o=create("core/organizations",Map.of("name",uuid(),"nature","Public safety","publicOrganization",true,"active",true)),u=create("core/units",Map.of("organizationId",o,"code",uuid(),"name","Unit","type","Unit")),ca=create("inventory/categories",Map.of("name",uuid(),"family","OPTICAL","serialized",true,"lotControlled",false,"consumable",false)),cl=create("inventory/categories",Map.of("name",uuid(),"family","AMMUNITION","serialized",false,"lotControlled",true,"consumable",true)),b=create("inventory/brands",Map.of("name",uuid(),"manufacturer","Maker")),ma=create("inventory/models",Map.of("categoryId",ca,"brandId",b,"name","Sight","unitOfMeasure","EA","sku",uuid(),"listPrice","1")),ml=create("inventory/models",Map.of("categoryId",cl,"brandId",b,"name","Cartridge","unitOfMeasure","EA","sku",uuid(),"listPrice","1")),l=create("inventory/locations",Map.of("organizationId",o,"unitId",u,"name","Vault","type","Controlled","controlled",true)),a=create("inventory/assets",Map.of("modelId",ma,"locationId",l,"assetCode",uuid(),"serialNumber",uuid(),"condition","GOOD","status","AVAILABLE","currentValue","1")),lot=create("inventory/lots",Map.of("modelId",ml,"openingLocationId",l,"lotNumber",uuid(),"initialQuantity","10"));long balance=jdbc.queryForObject("select id from erp_stock_balance where lot_id=?",Long.class,lot);return new S(o,u,l,a,lot,balance);}
 private String decimal(String q,Object...a){return jdbc.queryForObject(q,BigDecimal.class,a).setScale(4).toPlainString();}private long create(String p,Object b)throws Exception{var r=request("POST",p,b);assertEquals(201,r.status(),r.raw());return r.body().get("id").asLong();}private R request(String m,String p,Object b)throws Exception{var q=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/erp/"+p)).header("Content-Type","application/json").method(m,b==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(b))).build();var x=client.send(q,HttpResponse.BodyHandlers.ofString());return new R(x.statusCode(),x.body().isBlank()?null:json.readTree(x.body()),x.body());}private String uuid(){return UUID.randomUUID().toString();}}
