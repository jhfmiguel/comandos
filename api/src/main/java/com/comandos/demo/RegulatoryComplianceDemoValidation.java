package com.comandos.demo;
import com.fariamiguel.app.demo.DemoValidationContributor;
import com.fariamiguel.app.inventory.model.*;
import com.comandos.inventory.model.*;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RegulatoryComplianceDemoValidation implements DemoValidationContributor {
    private final EntityManager em;
    public RegulatoryComplianceDemoValidation(EntityManager em){this.em=em;}
    public String phase(){return "compliance";}
    @Override
    public void verify() {
        LocalDate today = LocalDate.now();
        for (RegulatoryControl control : em.createQuery("select r from RegulatoryControl r", RegulatoryControl.class).getResultList()) {
            require(control.asset != null && notBlank(control.externalSystem) && notBlank(control.registrationNumber) && notBlank(control.status),
                "Regulatory control must preserve asset, system, registration and status.");
            if (control.validUntil != null && control.validUntil.isBefore(today) && !AssetStatus.terminalCodes().contains(control.asset.status)) {
                require(!AssetStatus.AVAILABLE.name().equals(control.asset.status) || !Set.of("ACTIVE", "VALID", "COMPLIANT").contains(control.status.toUpperCase()),
                    "Expired regulatory control cannot present an unrestricted available asset as compliant.");
            }
        }
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    private static boolean notBlank(String value){return value!=null&&!value.isBlank();}
}