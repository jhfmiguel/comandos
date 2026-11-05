package com.comandos.inventory.service;
import com.fariamiguel.app.inventory.service.InventoryRules;

import com.fariamiguel.app.core.model.CoreEntity;
import com.fariamiguel.app.core.service.CanonicalMasterDataDirectory;
import com.fariamiguel.enterprise.catalog.CatalogTrackingPolicy;
import com.fariamiguel.enterprise.catalog.UnitOfMeasureCode;
import com.fariamiguel.app.inventory.model.*;
import com.comandos.inventory.model.*;
import com.fariamiguel.app.reservation.model.ReservationStatusType;
import com.fariamiguel.app.reconciliation.model.*;
import com.fariamiguel.app.sales.model.*;
import com.comandos.custody.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import com.fariamiguel.tenancy.api.CompanyId;
import com.fariamiguel.tenancy.api.TenantId;

@Component
@org.springframework.context.annotation.Primary
public class ComandosInventoryRules extends InventoryRules {
    private static final TenantId TENANT = TenantId.of("comandos");
    private final CanonicalMasterDataDirectory masterData;
    public ComandosInventoryRules(EntityManager em, CanonicalMasterDataDirectory masterData) {
        super(em);
        this.masterData = masterData;
    }

    @Override
    public void validate(CoreEntity entity, Map<String, Object> previous) {
        super.validate(entity, previous);
        if (entity instanceof ArmamentParameter parameter) {
            if (parameter.parameterType == null || parameter.parameterType.isBlank())
                bad("Parameter type is required.");
            parameter.code = parameter.code == null ? null : parameter.code.trim().toUpperCase(Locale.ROOT);
            parameter.name = parameter.name == null ? null : parameter.name.trim();
            registry(parameter.code, 0);
            if (parameter.name == null || parameter.name.isBlank())
                bad("Parameter name is required.");

            long duplicateCode = em.createQuery(
                    "select count(p) from ArmamentParameter p where p.parameterType = :type and p.code = :code and p.id <> :id",
                    Long.class)
                .setParameter("type", parameter.parameterType)
                .setParameter("code", parameter.code)
                .setParameter("id", parameter.id == null ? -1L : parameter.id)
                .setFlushMode(jakarta.persistence.FlushModeType.COMMIT)
                .getSingleResult();
            if (duplicateCode > 0) bad("A parameter with this code already exists.");

            long duplicateName = em.createQuery(
                    "select count(p) from ArmamentParameter p where p.parameterType = :type and lower(p.name) = lower(:name) and p.id <> :id",
                    Long.class)
                .setParameter("type", parameter.parameterType)
                .setParameter("name", parameter.name)
                .setParameter("id", parameter.id == null ? -1L : parameter.id)
                .setFlushMode(jakarta.persistence.FlushModeType.COMMIT)
                .getSingleResult();
            if (duplicateName > 0) bad("A parameter with this name already exists.");
        }
        if (entity instanceof ArmamentType type) {
            registry(type.code, 0);
            if (!type.code.matches("[A-Z][A-Z0-9_]{1,49}")) bad("Code must be uppercase.");
            if (type.id != null && typeUsage(type.id) > 0 && (!type.active
                    || !Objects.equals(previous.get("categoryId"), type.category.id)))
                bad("A used armament type cannot be deactivated or moved to another category. Reassign its references first.");
        }
        if (entity instanceof ArmamentClassification classification) {
            registry(classification.code, 0);
            if (!classification.code.matches("[A-Z][A-Z0-9_]{1,49}")) bad("Code must be uppercase.");
            if (!classification.type.active) bad("Select an active armament type.");
            if (classification.id != null && classificationUsage(classification.id) > 0 && (!classification.active
                    || !Objects.equals(previous.get("typeId"), classification.type.id)))
                bad("A used classification cannot be deactivated or moved to another type. Reassign its references first.");
        }
        if (entity instanceof FirearmSpecification specification) {
            specification.caliber = parameterName(specification.caliberRef, "CALIBER", "Caliber", true, specification.caliber);
            if (!"FIREARM".equals(specification.model.category.family))
                bad("Firearm specifications require a model in the FIREARM family.");
            if (specification.capacity == null || specification.capacity <= 0)
                bad("Capacity must be greater than zero.");
            if (specification.barrelLength == null || specification.barrelLength.signum() <= 0)
                bad("Barrel length must be greater than zero.");
        }
        if (entity instanceof AmmunitionSpecification specification) {
            specification.caliber = parameterName(specification.caliberRef, "CALIBER", "Caliber", true, specification.caliber);
            specification.ammunitionType = parameterName(specification.ammunitionTypeRef, "AMMUNITION_TYPE", "Ammunition type", true, specification.ammunitionType);
            specification.projectileType = parameterName(specification.projectileTypeRef, "PROJECTILE_TYPE", "Projectile type", true, specification.projectileType);
            specification.caseType = parameterName(specification.caseTypeRef, "CASE_TYPE", "Case type", true, specification.caseType);
            specification.primerType = parameterName(specification.primerTypeRef, "PRIMER_TYPE", "Primer type", true, specification.primerType);
            if (!"AMMUNITION".equals(specification.model.category.family))
                bad("Ammunition specifications require a model in the AMMUNITION family.");
            if (!Set.of("LETHAL", "LESS_LETHAL").contains(specification.lethalityClassification))
                bad("Lethality classification must be LETHAL or LESS_LETHAL.");
        }
        if (entity instanceof GrenadeSpecification specification) {
            specification.grenadeType = parameterName(specification.grenadeTypeRef, "GRENADE_TYPE", "Grenade type", true, specification.grenadeType);
            specification.agent = parameterName(specification.agentRef, "AGENT", "Agent", true, specification.agent);
            specification.composition = parameterName(specification.compositionRef, "COMPOSITION", "Composition", true, specification.composition);
            if (!"GRENADE".equals(specification.model.category.family))
                bad("Grenade specifications require a model in the GRENADE family.");
            if (specification.delaySeconds == null || specification.delaySeconds <= 0)
                bad("Delay time must be greater than zero.");
            if (specification.safetyRadius == null || specification.safetyRadius.signum() <= 0)
                bad("Safety radius must be greater than zero.");
            if (specification.shelfLifeMonths == null || specification.shelfLifeMonths <= 0)
                bad("Shelf life must be greater than zero.");
        }
        if (entity instanceof SpraySpecification specification) {
            specification.agent = parameterName(specification.agentRef, "AGENT", "Agent", true, specification.agent);
            specification.composition = parameterName(specification.compositionRef, "COMPOSITION", "Composition", true, specification.composition);
            if (!"SPRAY".equals(specification.model.category.family))
                bad("Spray specifications require a model in the SPRAY family.");
            if (specification.concentration == null || specification.concentration.signum() <= 0
                    || specification.concentration.compareTo(new BigDecimal("100")) > 0)
                bad("Concentration must be greater than zero and at most 100 percent.");
            if (specification.volumeMl == null || specification.volumeMl.signum() <= 0)
                bad("Volume must be greater than zero.");
            if (specification.rangeMeters == null || specification.rangeMeters.signum() <= 0)
                bad("Range must be greater than zero.");
            if (specification.shelfLifeMonths == null || specification.shelfLifeMonths <= 0)
                bad("Shelf life must be greater than zero.");
        }
        if (entity instanceof BallisticProtectionSpecification specification) {
            specification.protectionType = parameterName(specification.protectionTypeRef, "PROTECTION_TYPE", "Protection type", true, specification.protectionType);
            specification.protectionLevel = parameterName(specification.protectionLevelRef, "PROTECTION_LEVEL", "Protection level", true, specification.protectionLevel);
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true, specification.material);
            specification.size = parameterName(specification.sizeRef, "SIZE", "Size", true, specification.size);
            if (!"BALLISTIC_PROTECTION".equals(specification.model.category.family))
                bad("Ballistic protection specifications require a model in the BALLISTIC_PROTECTION family.");
            if (specification.serviceLifeMonths == null || specification.serviceLifeMonths <= 0)
                bad("Service life must be greater than zero.");
        }
        if (entity instanceof ElectricalDeviceSpecification specification) {
            specification.cartridgeType = parameterName(specification.cartridgeTypeRef, "CARTRIDGE_TYPE", "Cartridge type", true, specification.cartridgeType);
            if (!"ELECTRICAL_DEVICE".equals(specification.model.category.family))
                bad("Electrical device specifications require a model in the ELECTRICAL_DEVICE family.");
            if (specification.voltage == null || specification.voltage.signum() <= 0)
                bad("Voltage must be greater than zero.");
            if (specification.cycles == null || specification.cycles <= 0)
                bad("Cycles must be greater than zero.");
        }
        if (entity instanceof OpticalSpecification specification) {
            specification.opticalType = parameterName(specification.opticalTypeRef, "OPTICAL_TYPE", "Optical type", true, specification.opticalType);
            if (!"OPTICAL".equals(specification.model.category.family))
                bad("Optical specifications require a model in the OPTICAL family.");
            if (specification.minimumMagnification == null || specification.minimumMagnification.signum() <= 0)
                bad("Minimum magnification must be greater than zero.");
            if (specification.maximumMagnification == null || specification.maximumMagnification.signum() <= 0)
                bad("Maximum magnification must be greater than zero.");
            if (specification.maximumMagnification.compareTo(specification.minimumMagnification) < 0)
                bad("Maximum magnification cannot be lower than minimum magnification.");
            positiveOptional(specification.objectiveDiameterMm, "Objective diameter");
        }
        if (entity instanceof HelmetSpecification specification) {
            specification.protectionLevel = parameterName(specification.protectionLevelRef, "PROTECTION_LEVEL", "Protection level", true, specification.protectionLevel);
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true, specification.material);
            specification.size = parameterName(specification.sizeRef, "SIZE", "Size", true, specification.size);
            if (!"HELMET".equals(specification.model.category.family))
                bad("Helmet specifications require a model in the HELMET family.");
            positiveOptional(specification.weightGrams, "Helmet weight");
        }
        if (entity instanceof ShieldSpecification specification) {
            specification.shieldType = parameterName(specification.shieldTypeRef, "SHIELD_TYPE", "Shield type", true, specification.shieldType);
            specification.protectionLevel = parameterName(specification.protectionLevelRef, "PROTECTION_LEVEL", "Protection level", false, specification.protectionLevel);
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true, specification.material);
            if (!"SHIELD".equals(specification.model.category.family))
                bad("Shield specifications require a model in the SHIELD family.");
            positiveOptional(specification.heightMm, "Shield height");
            positiveOptional(specification.widthMm, "Shield width");
            positiveOptional(specification.weightGrams, "Shield weight");
        }
        if (entity instanceof RestraintSpecification specification) {
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", true, specification.material);
            specification.lockingMechanism = parameterName(specification.lockingMechanismRef, "LOCKING_MECHANISM", "Locking mechanism", false, specification.lockingMechanism);
            if (!"RESTRAINT".equals(specification.model.category.family))
                bad("Restraint specifications require a model in the RESTRAINT family.");
        }
        if (entity instanceof AccessoryComponentSpecification specification) {
            specification.componentType = parameterName(specification.componentTypeRef, "COMPONENT_TYPE", "Component type", true, specification.componentType);
            specification.compatibleWith = parameterName(specification.compatibilityRef, "COMPATIBILITY", "Compatibility", false, specification.compatibleWith);
            specification.mountingInterface = parameterName(specification.interfaceRef, "INTERFACE", "Interface", false, specification.mountingInterface);
            if (!"ACCESSORY_COMPONENT".equals(specification.model.category.family))
                bad("Accessory/component specifications require a model in the ACCESSORY_COMPONENT family.");
        }
        if (entity instanceof TacticalEquipmentSpecification specification) {
            specification.material = parameterName(specification.materialRef, "MATERIAL", "Material", false, specification.material);
            specification.size = parameterName(specification.sizeRef, "SIZE", "Size", false, specification.size);
            if (!"TACTICAL_EQUIPMENT".equals(specification.model.category.family))
                bad("Tactical equipment specifications require a model in the TACTICAL_EQUIPMENT family.");
            positiveOptional(specification.weightGrams, "Tactical equipment weight");
        }
        if (entity instanceof RegulatoryControl control) {
            if (!"FIREARM".equals(control.asset.model.category.family))
                bad("Regulatory controls require an individual asset in the FIREARM family.");
            if (AssetStatus.terminalCodes().contains(control.asset.status))
                bad("A regulatory registration cannot be created or changed for a terminal asset.");
            if ("ACTIVE".equals(control.status) && control.validUntil != null && control.validUntil.isBefore(LocalDate.now()))
                bad("An expired regulatory registration cannot remain active.");
        }
        if (entity instanceof CustodyReturnConditionType condition) {
            registry(condition.code, condition.displayOrder); condition.code = condition.code.trim().toUpperCase(Locale.ROOT);
            if (!previous.isEmpty() && !Objects.equals(previous.get("code"), condition.code)) bad("Condition code cannot be changed.");
            if (!previous.isEmpty() && Boolean.TRUE.equals(previous.get("systemProtected")) && !condition.systemProtected)
                bad("System protection cannot be removed.");
        }
        if (entity instanceof ItemModel model) {
            if (ArmamentModelDetails.of(em, model).armamentType != null && (!ArmamentModelDetails.of(em, model).armamentType.active
                    || !Objects.equals(ArmamentModelDetails.of(em, model).armamentType.category.id, model.category.id)))
                bad("Select an active armament type belonging to the model category.");
            if (ArmamentModelDetails.of(em, model).armamentClassification != null && (!ArmamentModelDetails.of(em, model).armamentClassification.active
                    || ArmamentModelDetails.of(em, model).armamentType == null || !Objects.equals(ArmamentModelDetails.of(em, model).armamentClassification.type.id, ArmamentModelDetails.of(em, model).armamentType.id)))
                bad("Select an active classification belonging to the model armament type.");
        }
    }

    @Override
    public void beforeDelete(CoreEntity entity) {
        super.beforeDelete(entity);
        if (entity instanceof ArmamentParameter parameter && parameterUsage(parameter.id) > 0)
            bad("A parameter currently used by a technical specification cannot be deleted. Deactivate it instead.");
        if (entity instanceof ArmamentType type && typeUsage(type.id) > 0)
            bad("A used armament type cannot be deleted.");
        if (entity instanceof ArmamentClassification classification && classificationUsage(classification.id) > 0)
            bad("A used armament classification cannot be deleted.");
        if (entity instanceof CustodyReturnConditionType condition && (condition.systemProtected
                || count("select count(i) from CustodyReturnItem i where i.conditionType.id = :id", condition.id) > 0))
            bad("A protected or used custody return condition cannot be deleted. Deactivate it instead.");
    }
    private String parameterName(
            ArmamentParameter parameter,
            String expectedType,
            String label,
            boolean required,
            String legacyValue) {
        if (parameter == null) {
            String normalizedLegacy = legacyValue == null ? null : legacyValue.trim();
            if (required && (normalizedLegacy == null || normalizedLegacy.isBlank()))
                bad(label + " is required.");
            return normalizedLegacy == null || normalizedLegacy.isBlank() ? null : normalizedLegacy;
        }
        if (!expectedType.equals(parameter.parameterType))
            bad(label + " belongs to an invalid parameter type.");
        return parameter.name;
    }

    private long specificationUsage(long id) {
        return count("select count(v) from FirearmSpecification v where v.model.id = :id", id)
            + count("select count(v) from AmmunitionSpecification v where v.model.id = :id", id)
            + count("select count(v) from GrenadeSpecification v where v.model.id = :id", id)
            + count("select count(v) from SpraySpecification v where v.model.id = :id", id)
            + count("select count(v) from BallisticProtectionSpecification v where v.model.id = :id", id)
            + count("select count(v) from ElectricalDeviceSpecification v where v.model.id = :id", id)
            + count("select count(v) from OpticalSpecification v where v.model.id = :id", id);
    }

    private long typeUsage(long id) {
        return count("select count(m) from ArmamentModelDetails m where m.armamentType.id = :id", id)
            + count("select count(c) from ArmamentClassification c where c.type.id = :id", id);
    }

    private long classificationUsage(long id) {
        return count("select count(m) from ArmamentModelDetails m where m.armamentClassification.id = :id", id);
    }

    private long parameterUsage(long id) {
        long total = 0;
        for (String query : List.of(
            "select count(v) from FirearmSpecification v where v.caliberRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.caliberRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.ammunitionTypeRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.projectileTypeRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.caseTypeRef.id = :id",
            "select count(v) from AmmunitionSpecification v where v.primerTypeRef.id = :id",
            "select count(v) from GrenadeSpecification v where v.grenadeTypeRef.id = :id",
            "select count(v) from GrenadeSpecification v where v.agentRef.id = :id",
            "select count(v) from GrenadeSpecification v where v.compositionRef.id = :id",
            "select count(v) from SpraySpecification v where v.agentRef.id = :id",
            "select count(v) from SpraySpecification v where v.compositionRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.protectionTypeRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.protectionLevelRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.materialRef.id = :id",
            "select count(v) from BallisticProtectionSpecification v where v.sizeRef.id = :id",
            "select count(v) from ElectricalDeviceSpecification v where v.cartridgeTypeRef.id = :id",
            "select count(v) from OpticalSpecification v where v.opticalTypeRef.id = :id",
            "select count(v) from HelmetSpecification v where v.protectionLevelRef.id = :id",
            "select count(v) from HelmetSpecification v where v.materialRef.id = :id",
            "select count(v) from HelmetSpecification v where v.sizeRef.id = :id",
            "select count(v) from ShieldSpecification v where v.shieldTypeRef.id = :id",
            "select count(v) from ShieldSpecification v where v.protectionLevelRef.id = :id",
            "select count(v) from ShieldSpecification v where v.materialRef.id = :id",
            "select count(v) from RestraintSpecification v where v.materialRef.id = :id",
            "select count(v) from RestraintSpecification v where v.lockingMechanismRef.id = :id",
            "select count(v) from AccessoryComponentSpecification v where v.componentTypeRef.id = :id",
            "select count(v) from AccessoryComponentSpecification v where v.compatibilityRef.id = :id",
            "select count(v) from AccessoryComponentSpecification v where v.interfaceRef.id = :id",
            "select count(v) from TacticalEquipmentSpecification v where v.materialRef.id = :id",
            "select count(v) from TacticalEquipmentSpecification v where v.sizeRef.id = :id"
        )) {
            total += count(query, id);
        }
        return total;
    }

    @Override
    protected boolean modelHasSpecifications(long id) { return specificationUsage(id) > 0; }

    @Override
    protected boolean workflowStatus(Object status) {
        return status != null && SecurityAssetStatus.workflowManagedCodes().contains(status.toString());
    }

    @Override
    protected void requireOrganization(long id) {
        if (masterData.findOrganization(id, TENANT).isEmpty()) bad("Organization not found.");
    }

    @Override
    protected Long organizationId(CoreEntity entity) {
        return switch (entity) {
            case StockLocation value -> value.organizationLegacyId;
            case EquipmentSet value -> value.organizationLegacyId;
            case ExpirationRecord value -> value.organizationLegacyId;
            case CertificationRecord value -> value.organizationLegacyId;
            case Recall value -> value.organizationLegacyId;
            default -> super.organizationId(entity);
        };
    }

    @Override
    protected Long unitId(CoreEntity entity) {
        return switch (entity) {
            case StockLocation value -> value.unitLegacyId;
            case EquipmentSet value -> value.unitLegacyId;
            case ExpirationRecord value -> value.unitLegacyId;
            case CertificationRecord value -> value.unitLegacyId;
            case Recall value -> value.unitLegacyId;
            default -> super.unitId(entity);
        };
    }

    @Override
    protected void requireUnitBelongsToOrganization(Long organizationId, Long unitId) {
        if (organizationId == null || organizationId <= 0) {
            bad("Organization is required.");
        }
        if (masterData.findOrganization(organizationId, TENANT).isEmpty()) {
            bad("Organization not found.");
        }
        if (unitId == null) return;

        var unit = masterData.findUnit(unitId, TENANT)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Organizational unit not found."
            ));

        CompanyId expectedCompany = CompanyId.of(
            "comandos:organization:" + organizationId
        );
        if (!expectedCompany.equals(unit.companyId())) {
            bad("Unit must belong to the selected organization.");
        }
    }
}