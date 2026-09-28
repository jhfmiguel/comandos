package com.comandos.demo;

import com.comandos.inventory.model.*;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "comandos.demo.seed", havingValue = "true")
@Order(1750)
public class CatalogFamilyDemoVerifier implements ApplicationRunner {

    private final EntityManager em;

    public CatalogFamilyDemoVerifier(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        Map<String, Class<?>> families = new LinkedHashMap<>();
        families.put("FIREARM", FirearmSpecification.class);
        families.put("AMMUNITION", AmmunitionSpecification.class);
        families.put("GRENADE", GrenadeSpecification.class);
        families.put("BALLISTIC_PROTECTION", BallisticProtectionSpecification.class);
        families.put("HELMET", HelmetSpecification.class);
        families.put("SHIELD", ShieldSpecification.class);
        families.put("SPRAY", SpraySpecification.class);
        families.put("ELECTRICAL_DEVICE", ElectricalDeviceSpecification.class);

        for (Map.Entry<String, Class<?>> entry : families.entrySet()) {
            verifyFamily(entry.getKey(), entry.getValue());
        }
    }

    private void verifyFamily(String family, Class<?> specificationType) {
        ItemCategory category = em.createQuery(
                "select c from ItemCategory c where c.family = :family", ItemCategory.class)
            .setParameter("family", family)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Catalog family is missing: " + family));

        ArmamentType type = em.createQuery(
                "select t from ArmamentType t where t.category.id = :categoryId and t.active = true order by t.id",
                ArmamentType.class)
            .setParameter("categoryId", category.id)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Catalog family has no active type: " + family));

        em.createQuery(
                "select c from ArmamentClassification c where c.type.id = :typeId and c.active = true order by c.id",
                ArmamentClassification.class)
            .setParameter("typeId", type.id)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Catalog family has no active classification: " + family));

        ItemModel model = em.createQuery(
                "select m from ItemModel m where m.category.id = :categoryId order by m.id", ItemModel.class)
            .setParameter("categoryId", category.id)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Catalog family has no test model: " + family));

        if (model.brand == null || model.armamentType == null || model.armamentClassification == null) {
            throw new IllegalStateException("Catalog model is not fully classified: " + family + "/" + model.sku);
        }
        if (!category.id.equals(model.armamentType.category.id)
            || !model.armamentType.id.equals(model.armamentClassification.type.id)) {
            throw new IllegalStateException("Catalog chained selection is inconsistent: " + family + "/" + model.sku);
        }
        if (model.listPrice == null || model.listPrice.signum() < 0 || blank(model.unitOfMeasure) || blank(model.sku)) {
            throw new IllegalStateException("Catalog model core data is incomplete: " + family + "/" + model.id);
        }

        Object spec = em.createQuery(
                "select s from " + specificationType.getSimpleName() + " s where s.model.id = :modelId",
                specificationType)
            .setParameter("modelId", model.id)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Catalog family has no technical specification: " + family + "/" + model.sku
            ));

        verifySpecification(family, spec);
    }

    private void verifySpecification(String family, Object spec) {
        switch (family) {
            case "FIREARM" -> {
                FirearmSpecification s = (FirearmSpecification) spec;
                require(s.caliberRef != null && !blank(s.operatingMechanism) && positive(s.capacity)
                    && positive(s.barrelLength), family);
            }
            case "AMMUNITION" -> {
                AmmunitionSpecification s = (AmmunitionSpecification) spec;
                require(s.caliberRef != null && s.ammunitionTypeRef != null && s.projectileTypeRef != null
                    && s.caseTypeRef != null && s.primerTypeRef != null && !blank(s.lethalityClassification), family);
            }
            case "GRENADE" -> {
                GrenadeSpecification s = (GrenadeSpecification) spec;
                require(s.grenadeTypeRef != null && s.agentRef != null && s.compositionRef != null
                    && positive(s.shelfLifeMonths) && positive(s.delaySeconds) && positive(s.safetyRadius), family);
            }
            case "BALLISTIC_PROTECTION" -> {
                BallisticProtectionSpecification s = (BallisticProtectionSpecification) spec;
                require(s.protectionTypeRef != null && s.protectionLevelRef != null && s.materialRef != null
                    && s.sizeRef != null && positive(s.serviceLifeMonths) && !blank(s.certification), family);
            }
            case "HELMET" -> {
                HelmetSpecification s = (HelmetSpecification) spec;
                require(s.protectionLevelRef != null && s.materialRef != null && s.sizeRef != null
                    && positive(s.weightGrams) && !blank(s.certification), family);
            }
            case "SHIELD" -> {
                ShieldSpecification s = (ShieldSpecification) spec;
                require(s.shieldTypeRef != null && s.materialRef != null && positive(s.heightMm)
                    && positive(s.widthMm) && positive(s.weightGrams), family);
            }
            case "SPRAY" -> {
                SpraySpecification s = (SpraySpecification) spec;
                require(s.agentRef != null && s.compositionRef != null && positive(s.shelfLifeMonths)
                    && positive(s.concentration) && positive(s.volumeMl) && positive(s.rangeMeters), family);
            }
            case "ELECTRICAL_DEVICE" -> {
                ElectricalDeviceSpecification s = (ElectricalDeviceSpecification) spec;
                require(positive(s.voltage) && positive(s.cycles) && s.cartridgeTypeRef != null, family);
            }
            default -> throw new IllegalStateException("Unsupported catalog family verifier: " + family);
        }
    }

    private static void require(boolean condition, String family) {
        if (!condition) throw new IllegalStateException("Catalog technical parameters are incomplete: " + family);
    }

    private static boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
