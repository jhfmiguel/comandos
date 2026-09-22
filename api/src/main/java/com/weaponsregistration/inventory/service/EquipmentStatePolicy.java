package com.weaponsregistration.inventory.service;
import com.weaponsregistration.inventory.model.AssetItem;import java.util.*;import org.springframework.http.*;import org.springframework.stereotype.Component;import org.springframework.web.server.ResponseStatusException;
@Component public class EquipmentStatePolicy{
 private static final Map<String,Set<String>>ALLOWED=Map.of(
  "CUSTODY",Set.of("AVAILABLE"),"TRANSFER",Set.of("AVAILABLE"),"MAINTENANCE",Set.of("AVAILABLE","BLOCKED"),
  "SALE",Set.of("AVAILABLE","BLOCKED"),"DISPOSAL",Set.of("AVAILABLE","BLOCKED","MISSING","RESTRICTED"),
  "INSPECTION",Set.of("AVAILABLE","CUSTODIED","IN_MAINTENANCE","BLOCKED"),"OCCURRENCE",Set.of("AVAILABLE","CUSTODIED","IN_MAINTENANCE","BLOCKED","MISSING","RESTRICTED"));
 public void require(AssetItem a,String operation){if(a==null)bad("Asset is required.");var states=ALLOWED.get(operation.toUpperCase(Locale.ROOT));if(states==null)bad("Unsupported equipment operation.");if(!states.contains(a.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Operation "+operation+" is incompatible with asset status "+a.status+".");}
 private static void bad(String m){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
}