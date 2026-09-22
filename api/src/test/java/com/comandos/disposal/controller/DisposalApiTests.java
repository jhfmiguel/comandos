package com.comandos.disposal.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:disposal-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","logging.level.root=WARN","debug=false"})
class DisposalApiTests {
 @Test void requiresExplicitConfirmation() throws Exception {
  var s=setup(); var data=new HashMap<String,Object>(payload(s));
  for (Boolean confirmed : Arrays.asList(null, false)) {
   data.put("confirmed",confirmed);
   assertEquals(400,request("POST","disposals",data).status());
  }
  assertEquals("AVAILABLE",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));
 }
 @Test void activeInventoryBlocksAssetsAndLotsUntilCancelled() throws Exception {
  var s=setup(); var opened=request("POST","inventory-counts",Map.of("requestId",unique(),"organizationId",s.org(),"unitId",s.unit(),"locationId",s.location(),"purpose","Count before disposal"));
  assertEquals(200,opened.status(),opened.raw());
  for (String kind : List.of("ASSET","LOT")) {
   assertEquals(0,request("GET","disposals/stock?organizationId="+s.org()+"&kind="+kind,null).body().get("totalElements").asInt());
   var data=new HashMap<String,Object>(payload(s));
   data.put("items",List.of(kind.equals("ASSET")?Map.of("assetId",s.asset(),"quantity",1):Map.of("balanceId",s.balance(),"quantity",1)));
   assertEquals(409,request("POST","disposals",data).status());
  }
  assertEquals(200,request("POST","inventory-counts/"+opened.body().get("id").asLong()+"/cancel",null).status());
  assertEquals(200,request("POST","disposals",payload(s)).status());
 }
 @Test void unavailableAssetsAndReservedBalancesCannotBeDisposed() throws Exception {
  var s=setup();
  for (String status : List.of("CUSTODIED","IN_MAINTENANCE","BLOCKED","DISPOSED")) {
   jdbc.update("update erp_asset_item set status=? where id=?",status,s.asset());
   assertEquals(409,request("POST","disposals",payload(s)).status(),status);
  }
  jdbc.update("update erp_asset_item set status='AVAILABLE' where id=?",s.asset());
  jdbc.update("update erp_stock_balance set reserved=1,available=9 where id=?",s.balance());
  assertEquals(409,request("POST","disposals",payload(s)).status());
  assertEquals("AVAILABLE",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));
 }
 @Test void logicalDisposalCannotBeReactivatedOrDisposedAgain() throws Exception {
  var s=setup(); var data=new HashMap<String,Object>(payload(s));
  authenticate();
  var recovery=request("POST","lifecycle/occurrences",Map.of("organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"type","RECOVERY","description","Pending recovery"));
  assertEquals(201,recovery.status(),recovery.raw());
  data.remove("destructionMethod");data.remove("destroyedAt");data.remove("destructionCertificate");
  var result=request("POST","disposals",data);assertEquals(200,result.status(),result.raw());
  assertEquals(0,jdbc.queryForObject("select count(*) from erp_destruction where process_id=?",Integer.class,result.body().get("id").asLong()));
  var asset=request("GET","inventory/assets/"+s.asset(),null).body();
  var edit=json.readValue(asset.toString(),HashMap.class);edit.put("status","AVAILABLE");
  assertEquals(400,request("PUT","inventory/assets/"+s.asset(),edit).status());
  assertEquals(409,request("POST","disposals",payload(s)).status());
  assertEquals(409,request("POST","lifecycle/occurrences/"+recovery.body().get("id").asLong()+"/resolve",Map.of()).status());
  assertEquals(409,request("POST","lifecycle/occurrences",Map.of("organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"type","DAMAGE","description","Cannot unblock disposal")).status());
  assertEquals(409,request("POST","lifecycle/inspections",Map.of("organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"checklist","Validation","result","FAILED")).status());
  assertEquals("DISPOSED",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));
 }
 @LocalServerPort int port;@Autowired JdbcTemplate jdbc;private final HttpClient client=HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL)).build();private final JsonMapper json=JsonMapper.builder().build();record Result(int status,JsonNode body,String raw){}record Setup(long org,long unit,long location,long asset,long lot,long balance){}
 @Test void finalizesDisposalWithDestructionIdempotently()throws Exception{var s=setup();var data=payload(s);var first=request("POST","disposals",data);assertEquals(200,first.status(),first.raw());long id=first.body().get("id").asLong();assertEquals("DISPOSED",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));
        com.comandos.audit.controller.AuditTraceAssertions.trace(port,"disposals",id,"assetId="+s.asset()+"&lotId="+s.lot()+"&unitId="+s.unit(),"FINALIZE");assertEquals("7.5000",decimal("select available from erp_stock_balance where id=?",s.balance()));assertEquals("7.5000",decimal("select available_quantity from erp_stock_lot where id=?",s.lot()));assertEquals(2,jdbc.queryForObject("select count(*) from erp_disposal_item where process_id=?",Integer.class,id));assertEquals(1,jdbc.queryForObject("select count(*) from erp_destruction where process_id=?",Integer.class,id));assertEquals(id,request("POST","disposals",data).body().get("id").asLong());assertEquals(1,jdbc.queryForObject("select count(*) from erp_audit_record where resource='disposals' and record_id=?",Integer.class,id));}
 @Test void invalidSecondItemRollsBackEverything()throws Exception{var s=setup();var data=new HashMap<String,Object>(payload(s));data.put("items",List.of(Map.of("assetId",s.asset(),"quantity",1),Map.of("balanceId",s.balance(),"quantity",20)));assertEquals(409,request("POST","disposals",data).status());assertEquals("AVAILABLE",jdbc.queryForObject("select status from erp_asset_item where id=?",String.class,s.asset()));assertEquals("10.0000",decimal("select available from erp_stock_balance where id=?",s.balance()));assertEquals(0,jdbc.queryForObject("select count(*) from erp_disposal_process where request_id=?",Integer.class,data.get("requestId")));assertEquals(0,jdbc.queryForObject("select count(*) from erp_stock_movement where nature='DISPOSAL' and (asset_id=? or lot_id=?)",Integer.class,s.asset(),s.lot()));}
 @Test void validatesDestructionGroupAndExposesHistory()throws Exception{var s=setup();var invalid=new HashMap<String,Object>(payload(s));invalid.remove("destructionCertificate");assertEquals(400,request("POST","disposals",invalid).status());assertEquals(1,request("GET","disposals/stock?organizationId="+s.org()+"&unitId="+s.unit()+"&kind=ASSET",null).body().get("totalElements").asInt());var created=request("POST","disposals",payload(s));assertEquals(200,created.status(),created.raw());assertEquals(1,request("GET","disposals?organizationId="+s.org()+"&unitId="+s.unit(),null).body().get("totalElements").asInt());assertEquals(created.body().get("id").asLong(),request("GET","disposals/"+created.body().get("id").asLong(),null).body().get("id").asLong());}
 private void authenticate() throws Exception {
  long person=create("core/people",Map.of("personType","INDIVIDUAL","fullName","Disposal operator","active",true));
  String login=unique();create("core/users",Map.of("personId",person,"login",login,"password","Disposal-test-password","blocked",false));
  var tokenResponse=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
  var token=json.readTree(tokenResponse.body());
  var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/login"))
   .header(token.get("headerName").asText(),token.get("token").asText()).header("Content-Type","application/x-www-form-urlencoded")
   .POST(HttpRequest.BodyPublishers.ofString("username="+login+"&password=Disposal-test-password")).build(),HttpResponse.BodyHandlers.ofString());
  assertEquals(204,response.statusCode(),response.body());
 }
 private Setup setup()throws Exception{long org=create("core/organizations",Map.of("name",unique(),"nature","Public safety","publicOrganization",true,"active",true));long unit=create("core/units",Map.of("organizationId",org,"code",unique(),"name","Unit","type","Unit"));long category=create("inventory/categories",Map.of("name",unique(),"family","GENERAL","serialized",true,"lotControlled",false,"consumable",false));long lotCategory=create("inventory/categories",Map.of("name",unique(),"family","GENERAL","serialized",false,"lotControlled",true,"consumable",true));long brand=create("inventory/brands",Map.of("name",unique(),"manufacturer","Maker"));long model=create("inventory/models",Map.of("categoryId",category,"brandId",brand,"name","Asset","unitOfMeasure","EA","sku",unique(),"listPrice","10"));long lotModel=create("inventory/models",Map.of("categoryId",lotCategory,"brandId",brand,"name","Supply","unitOfMeasure","EA","sku",unique(),"listPrice","1"));long location=create("inventory/locations",Map.of("organizationId",org,"unitId",unit,"name","Store","type","Controlled","controlled",true));long asset=create("inventory/assets",Map.of("modelId",model,"locationId",location,"assetCode",unique(),"serialNumber",unique(),"condition","DAMAGED","status","AVAILABLE","currentValue","0"));long lot=create("inventory/lots",Map.of("modelId",lotModel,"openingLocationId",location,"lotNumber",unique(),"initialQuantity","10"));long balance=jdbc.queryForObject("select id from erp_stock_balance where lot_id=?",Long.class,lot);return new Setup(org,unit,location,asset,lot,balance);}
 private Map<String,Object> payload(Setup s){return Map.of("confirmed",true,"requestId",unique(),"organizationId",s.org(),"unitId",s.unit(),"processNumber",unique(),"reason","Unserviceable stock","destructionMethod","Controlled dismantling","destroyedAt","2026-09-11T12:30:00","destructionCertificate","CERT-001","items",List.of(Map.of("assetId",s.asset(),"quantity",1),Map.of("balanceId",s.balance(),"quantity","2.5000")));}
 private Result request(String method,String path,Object body)throws Exception{var req=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/erp/"+path)).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();var response=client.send(req,HttpResponse.BodyHandlers.ofString());return new Result(response.statusCode(),response.body().isBlank()?null:json.readTree(response.body()),response.body());}
 private long create(String path,Map<String,Object> body)throws Exception{var result=request("POST",path,body);assertEquals(201,result.status(),result.raw());return result.body().get("id").asLong();}private String decimal(String sql,Object...args){return jdbc.queryForObject(sql,BigDecimal.class,args).setScale(4).toPlainString();}private String unique(){return UUID.randomUUID().toString();}
}
