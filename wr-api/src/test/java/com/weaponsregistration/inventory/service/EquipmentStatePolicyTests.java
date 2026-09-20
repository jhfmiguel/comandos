package com.weaponsregistration.inventory.service;
import com.weaponsregistration.inventory.model.AssetItem;import org.junit.jupiter.api.Test;import org.springframework.web.server.ResponseStatusException;import static org.junit.jupiter.api.Assertions.*;
class EquipmentStatePolicyTests{private final EquipmentStatePolicy policy=new EquipmentStatePolicy();
 @Test void custodyRequiresAvailableAsset(){var a=new AssetItem();a.status="MAINTENANCE";assertThrows(ResponseStatusException.class,()->policy.require(a,"CUSTODY"));a.status="AVAILABLE";assertDoesNotThrow(()->policy.require(a,"CUSTODY"));}
 @Test void maintenanceAllowsBlockedAsset(){var a=new AssetItem();a.status="BLOCKED";assertDoesNotThrow(()->policy.require(a,"MAINTENANCE"));}
 @Test void unsupportedOperationIsRejected(){var a=new AssetItem();a.status="AVAILABLE";assertThrows(ResponseStatusException.class,()->policy.require(a,"UNKNOWN"));}
}