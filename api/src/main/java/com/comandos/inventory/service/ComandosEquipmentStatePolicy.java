package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.EquipmentStatePolicy;

import com.fariamiguel.app.inventory.model.AssetStatus;
import com.comandos.inventory.model.SecurityAssetStatus;
import java.util.HashSet;
import java.util.Set;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Security operations extend the reusable equipment state policy. */
@Component
@Primary
public class ComandosEquipmentStatePolicy extends EquipmentStatePolicy {
    @Override
    protected String normalizeOperation(String operation) {
        if (operation != null && "CUSTODY".equalsIgnoreCase(operation.trim())) return "CUSTODY";
        return super.normalizeOperation(operation);
    }

    @Override
    protected String normalizeStatus(String status) {
        return SecurityAssetStatus.parseCode(status);
    }

    @Override
    protected Set<String> allowedStates(String operation) {
        if ("CUSTODY".equals(operation)) return Set.of(AssetStatus.AVAILABLE.name());
        Set<String> states = super.allowedStates(operation);
        if ("INSPECTION".equals(operation) || "OCCURRENCE".equals(operation)) {
            var extended = new HashSet<>(states);
            extended.add(SecurityAssetStatus.CUSTODIED.name());
            return Set.copyOf(extended);
        }
        return states;
    }
}