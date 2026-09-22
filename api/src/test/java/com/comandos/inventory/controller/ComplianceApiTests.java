package com.comandos.inventory.controller;import static org.junit.jupiter.api.Assertions.*;import java.net.*;import java.net.http.*;import java.util.*;import org.junit.jupiter.api.Test;import org.springframework.boot.test.context.SpringBootTest;import org.springframework.boot.test.web.server.LocalServerPort;import tools.jackson.databind.*;import tools.jackson.databind.json.JsonMapper;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.datasource.url=jdbc:h2:mem:compliance-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1","logging.level.root=WARN","debug=false"})class ComplianceApiTests{
 @LocalServerPort int port;final HttpClient client=HttpClient.newHttpClient();final JsonMapper json=JsonMapper.builder().build();record R(int status,JsonNode body,String raw){}record S(long org,long unit,long asset,long lot){}
 @Test void registersExpirationCertificationAndRecall()throws Exception{var s=setup();long expiration=create("inventory/expirations",Map.of("organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"type","SERVICE_LIFE","expirationDate","2025-01-01","status","EXPIRED"));assertEquals("BLOCKED",request("GET","inventory/assets/"+s.asset(),null).body().get("status").asText());create("inventory/certifications",Map.of("organizationId",s.org(),"unitId",s.unit(),"lotId",s.lot(),"type","CONFORMITY","number",uuid(),"validUntil","2028-01-01","status","ACTIVE"));long recall=create("inventory/recalls",Map.of("organizationId",s.org(),"unitId",s.unit(),"number",uuid(),"reason","Manufacturer safety notice","status","OPEN"));create("inventory/recall-items",Map.of("recallId",recall,"assetId",s.asset(),"action","Inspect and replace component"));assertEquals(1,request("GET","inventory/expirations?organizationId="+s.org(),null).body().get("totalElements").asInt());assertEquals(400,request("DELETE","inventory/expirations/"+expiration+"?version=0",null).status());}
 @Test void rejectsAmbiguousTargetsAndExpiredActiveCertificates()throws Exception{var s=setup();assertEquals(400,request("POST","inventory/expirations",Map.of("organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"lotId",s.lot(),"type","SERVICE_LIFE","expirationDate","2028-01-01","status","VALID")).status());assertEquals(400,request("POST","inventory/certifications",Map.of("organizationId",s.org(),"unitId",s.unit(),"assetId",s.asset(),"type","CONFORMITY","number",uuid(),"validUntil","2025-01-01","status","ACTIVE")).status());}

 @Test void recallDescriptionsRoundTripValidateSearchAndAudit() throws Exception {
  var scope=setup();
  var recall=new HashMap<String,Object>(Map.of("organizationId",scope.org(),"unitId",scope.unit(),"number",uuid(),"reason","Safety notice","status","OPEN"));
  long recallId=exerciseDescription("recalls",recall,scope.org());
  exerciseDescription("recall-items",new HashMap<>(Map.of("recallId",recallId,"assetId",scope.asset(),"action","Inspect")),scope.org());
  var catalog=request("GET","inventory/catalog",null);
  assertEquals(200,catalog.status());
  for(String key:List.of("recalls","recall-items")) {
   boolean found=false;
   for(var resource:catalog.body()) if(key.equals(resource.get("key").asText())) {
    for(var field:resource.get("fields")) if("description".equals(field.get("name").asText())) {
     found=true; assertEquals("text",field.get("type").asText());
     assertFalse(field.get("required").asBoolean()); assertFalse(field.get("readOnly").asBoolean()); assertFalse(field.get("createOnly").asBoolean());
    }
   }
   assertTrue(found,key);
  }
 }
 private long exerciseDescription(String resource,HashMap<String,Object> data,long org) throws Exception {
  String path="inventory/"+resource;
  // Legacy payloads omit description and continue to work.
  long id=create(path,data);
  assertTrue(request("GET",path+"/"+id,null).body().get("description").isNull());
  String description="description-"+uuid();
  data.put("version",0); data.put("description","  "+description+"  ");
  var updated=request("PUT",path+"/"+id,data); assertEquals(200,updated.status(),updated.raw());
  assertEquals(description,updated.body().get("description").asText());
  assertEquals(description,request("GET",path+"/"+id,null).body().get("description").asText());
  for(String query:List.of("search=","filter.description=")) {
   var list=request("GET",path+"?organizationId="+org+"&"+query+description,null);
   assertEquals(200,list.status(),list.raw()); assertEquals(1,list.body().get("totalElements").asInt());
   assertEquals(description,list.body().get("content").get(0).get("description").asText());
  }
  var events=request("GET","audit?resource="+path+"&recordId="+id,null).body();
  assertEquals(2,events.get("totalElements").asInt());
  var audit=request("GET","audit/"+events.get("content").get(0).get("id").asLong(),null).body();
  assertTrue(audit.get("before").get("description").isNull()); assertEquals(description,audit.get("after").get("description").asText());
  data.put("version",updated.body().get("version").asLong()); data.remove("description");
  updated=request("PUT",path+"/"+id,data); assertEquals(200,updated.status(),updated.raw());
  assertEquals(description,updated.body().get("description").asText());
  data.put("version",updated.body().get("version").asLong());
  for(Object invalid:List.of("x".repeat(256),123)) {
   data.put("description",invalid); assertEquals(400,request("PUT",path+"/"+id,data).status());
   assertEquals(description,request("GET",path+"/"+id,null).body().get("description").asText());
  }
  for(String value:Arrays.asList("x".repeat(255),"", "   ",null)) {
   data.put("description",value);
   updated=request("PUT",path+"/"+id,data); assertEquals(200,updated.status(),updated.raw());
   if(value==null||value.isBlank()) assertTrue(updated.body().get("description").isNull());
   else assertEquals(value,updated.body().get("description").asText());
   data.put("version",updated.body().get("version").asLong());
  }
  return id;
 }
 @Test void descriptionsAreAcceptedOnCreateAndOversizedCreatesAreRejected() throws Exception {
  var scope=setup();
  var recall=new HashMap<String,Object>(Map.of("organizationId",scope.org(),"number",uuid(),"reason","Safety","status","OPEN","description","x".repeat(256)));
  assertEquals(400,request("POST","inventory/recalls",recall).status());
  recall.put("description","x".repeat(255)); long id=create("inventory/recalls",recall);
  assertEquals("x".repeat(255),request("GET","inventory/recalls/"+id,null).body().get("description").asText());
  var item=new HashMap<String,Object>(Map.of("recallId",id,"lotId",scope.lot(),"action","Inspect","description","x".repeat(256)));
  assertEquals(400,request("POST","inventory/recall-items",item).status());
  item.put("description","  Lot details  "); long itemId=create("inventory/recall-items",item);
  assertEquals("Lot details",request("GET","inventory/recall-items/"+itemId,null).body().get("description").asText());
 }
 private S setup()throws Exception{long o=create("core/organizations",Map.of("name",uuid(),"nature","Public safety","publicOrganization",true,"active",true)),u=create("core/units",Map.of("organizationId",o,"code",uuid(),"name","Unit","type","Unit")),ca=create("inventory/categories",Map.of("name",uuid(),"family","FIREARM","serialized",true,"lotControlled",false,"consumable",false)),cl=create("inventory/categories",Map.of("name",uuid(),"family","AMMUNITION","serialized",false,"lotControlled",true,"consumable",true)),b=create("inventory/brands",Map.of("name",uuid(),"manufacturer","Maker")),ma=create("inventory/models",Map.of("categoryId",ca,"brandId",b,"name","Pistol","unitOfMeasure","EA","sku",uuid(),"listPrice","1")),ml=create("inventory/models",Map.of("categoryId",cl,"brandId",b,"name","Cartridge","unitOfMeasure","EA","sku",uuid(),"listPrice","1")),l=create("inventory/locations",Map.of("organizationId",o,"unitId",u,"name","Vault","type","Controlled","controlled",true)),a=create("inventory/assets",Map.of("modelId",ma,"locationId",l,"assetCode",uuid(),"serialNumber",uuid(),"condition","GOOD","status","AVAILABLE","currentValue","1")),lot=create("inventory/lots",Map.of("modelId",ml,"openingLocationId",l,"lotNumber",uuid(),"initialQuantity","10"));return new S(o,u,a,lot);}
 private long create(String p,Object b)throws Exception{var r=request("POST",p,b);assertEquals(201,r.status(),r.raw());return r.body().get("id").asLong();}private R request(String m,String p,Object b)throws Exception{var q=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/erp/"+p)).header("Content-Type","application/json").method(m,b==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(b))).build();var x=client.send(q,HttpResponse.BodyHandlers.ofString());return new R(x.statusCode(),x.body().isBlank()?null:json.readTree(x.body()),x.body());}private String uuid(){return UUID.randomUUID().toString();}
}
