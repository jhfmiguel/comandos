package com.comandos.demo;
import com.fariamiguel.app.demo.DemoValueContributor;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ComandosDemoValues implements DemoValueContributor {
    @Override
    public Map<String, String> values() {
        return Map.of(
            "access.profileName", "Gestor de Armamento - demonstração",
            "access.qualificationCategory", "Gestão e controle de armamento - treinamento",
            "status.Custody", "ACTIVE",
            "status.AmmunitionConsumption", "FINALIZED",
            "type.AmmunitionConsumption", "TRAINING",
            "type.Custody", "INDIVIDUAL",
            "unitName", "Armamento Central"
        );
    }
}